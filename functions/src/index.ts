import * as functions from 'firebase-functions';
import * as admin from 'firebase-admin';
import { GoogleGenerativeAI } from '@google/generative-ai';

// Initialize Firebase Admin
admin.initializeApp();

// Lazy initialize Gemini AI client to avoid throwing at import time
let genAI: any | null = null;
function getGenAI() {
  if (genAI) return genAI;

  const apiKey = functions.config().gemini?.api_key || process.env.GEMINI_API_KEY;
  if (!apiKey) {
    // Do not throw here (import time). Caller should catch and return friendly error.
    throw new Error('GEMINI_API_KEY_MISSING');
  }

  genAI = new GoogleGenerativeAI(apiKey);
  return genAI;
}

// Rate limiting storage (in production, use Redis or similar)
const userRequestCounts = new Map<string, { count: number; resetTime: number }>();

// Rate limit: 10 requests per hour per user
const RATE_LIMIT_REQUESTS = 10;
const RATE_LIMIT_WINDOW_MS = 60 * 60 * 1000; // 1 hour

interface GeminiFoodAnalysisRequest {
  foodName: string;
  userId?: string;
}

interface GeminiFoodAnalysisResponse {
  id: string;
  name: string;
  normalizedName: string;
  category: string;
  calories: number;
  carbs: number;
  fiber: number;
  sugars: number;
  protein: number;
  totalFat: number;
  saturatedFat: number;
  sodium: number;
  potassium: number;
  glycemicIndex?: number;
  glycemicLoad?: number;
  recommendations: { [condition: string]: ConditionRecommendation };
  primaryAlternatives: Alternative[];
  alternativeReasoning: string;
  glycemicImpact: string;
  nutritionalDensity: string;
}

interface ConditionRecommendation {
  safetyLevel: string;
  reasoning: string;
  keyPoints: string[];
  servingAdvice: string;
  timingAdvice?: string;
  pairingSuggestions: string[];
  alternatives: string[];
  bloodSugarImpact?: string;
  bloodPressureImpact?: string;
  heartHealthImpact?: string;
}

interface Alternative {
  foodId: string;
  advantage: string;
  improvement: string;
  bestFor: string[];
}

/**
 * Check if user is within rate limits
 */
function checkRateLimit(userId: string): boolean {
  const now = Date.now();
  const userLimit = userRequestCounts.get(userId);

  if (!userLimit || now > userLimit.resetTime) {
    // Reset or initialize rate limit
    userRequestCounts.set(userId, { count: 1, resetTime: now + RATE_LIMIT_WINDOW_MS });
    return true;
  }

  if (userLimit.count >= RATE_LIMIT_REQUESTS) {
    return false;
  }

  userLimit.count++;
  return true;
}

/**
 * Validate food name input
 */
function validateFoodName(foodName: string): boolean {
  if (!foodName || typeof foodName !== 'string') return false;
  if (foodName.trim().length < 2 || foodName.trim().length > 100) return false;

  // Check for potentially harmful input
  const harmfulPatterns = [
    /<script/i,
    /javascript:/i,
    /on\w+\s*=/i,
    /<iframe/i,
    /<object/i,
    /<embed/i
  ];

  return !harmfulPatterns.some(pattern => pattern.test(foodName));
}

/**
 * Generate normalized food name
 */
function generateNormalizedName(name: string): string {
  return name.toLowerCase()
    .trim()
    .replace(/[^a-zA-Z0-9\s]/g, ' ')
    .replace(/\s+/g, ' ')
    .trim();
}

/**
 * Generate food ID from name
 */
function generateFoodId(name: string): string {
  return generateNormalizedName(name)
    .replace(/\s+/g, '_')
    .replace(/[^a-zA-Z0-9_]/g, '')
    .substring(0, 50);
}

/**
 * Create comprehensive Gemini prompt for food analysis
 */
function createGeminiPrompt(foodName: string): string {
  return `Analyze "${foodName}" for diabetic and heart patients with focus on alternatives.

Provide a detailed analysis in the following structured JSON format. Be medically accurate and evidence-based:

{
  "basicInfo": {
    "category": "food category (e.g., fruit, vegetable, grain, protein, dairy, etc.)",
    "estimatedNutrition": {
      "calories": "per 100g serving",
      "carbs": "grams per 100g",
      "fiber": "grams per 100g",
      "sugars": "grams per 100g",
      "protein": "grams per 100g",
      "totalFat": "grams per 100g",
      "saturatedFat": "grams per 100g",
      "sodium": "mg per 100g",
      "potassium": "mg per 100g"
    },
    "glycemicInfo": {
      "glycemicIndex": "GI value (0-100, or null if unknown)",
      "glycemicLoad": "GL value (or null if unknown)",
      "glycemicImpact": "Low/Medium/High impact description"
    }
  },
  "medicalAnalysis": {
    "diabetes_type_1": {
      "safetyLevel": "Safe/Caution/Avoid",
      "reasoning": "detailed medical reasoning",
      "keyPoints": ["point 1", "point 2", "point 3"],
      "servingAdvice": "recommended serving size and frequency",
      "timingAdvice": "best time to consume",
      "pairingSuggestions": ["pair with this", "avoid pairing with that"],
      "bloodSugarImpact": "expected blood sugar effect"
    },
    "diabetes_type_2": {
      "safetyLevel": "Safe/Caution/Avoid",
      "reasoning": "detailed medical reasoning",
      "keyPoints": ["point 1", "point 2", "point 3"],
      "servingAdvice": "recommended serving size and frequency",
      "timingAdvice": "best time to consume",
      "pairingSuggestions": ["pair with this", "avoid pairing with that"],
      "bloodSugarImpact": "expected blood sugar effect"
    },
    "hypertension": {
      "safetyLevel": "Safe/Caution/Avoid",
      "reasoning": "detailed medical reasoning",
      "keyPoints": ["point 1", "point 2", "point 3"],
      "servingAdvice": "recommended serving size and frequency",
      "pairingSuggestions": ["pair with this", "avoid pairing with that"],
      "bloodPressureImpact": "expected blood pressure effect"
    },
    "heart_disease": {
      "safetyLevel": "Safe/Caution/Avoid",
      "reasoning": "detailed medical reasoning",
      "keyPoints": ["point 1", "point 2", "point 3"],
      "servingAdvice": "recommended serving size and frequency",
      "pairingSuggestions": ["pair with this", "avoid pairing with that"],
      "heartHealthImpact": "expected heart health effect"
    }
  },
  "alternatives": {
    "reasoning": "why these alternatives are suggested",
    "suggestions": [
      {
        "foodName": "specific alternative food",
        "advantage": "why this is better than original",
        "improvement": "specific health improvement",
        "bestFor": ["diabetes", "hypertension", "heart_disease"],
        "swapTip": "how to use as replacement"
      }
    ]
  },
  "nutritionalDensity": "High/Medium/Low overall nutritional value",
  "preparationTips": ["tip 1", "tip 2", "tip 3"]
}

IMPORTANT:
- Be medically accurate and evidence-based
- Focus on practical, actionable advice
- Consider both type 1 and type 2 diabetes
- Include 3-4 alternative suggestions
- Provide specific serving recommendations
- Explain reasoning for all recommendations
- Use realistic nutritional estimates per 100g serving`;
}

/**
 * Parse and validate Gemini response
 */
function parseGeminiResponse(response: string, foodName: string): GeminiFoodAnalysisResponse | null {
  try {
    // Extract JSON from response (Gemini might add extra text)
    const jsonMatch = response.match(/\{[\s\S]*\}/);
    if (!jsonMatch) {
      console.error('No JSON found in Gemini response');
      return null;
    }

    const parsed = JSON.parse(jsonMatch[0]);

    // Validate required fields
    if (!parsed.basicInfo || !parsed.medicalAnalysis || !parsed.alternatives) {
      console.error('Missing required fields in Gemini response');
      return null;
    }

    const foodId = generateFoodId(foodName);

    // Transform to our data structure
    const recommendations: { [condition: string]: ConditionRecommendation } = {};

    // Map diabetes type 1
    if (parsed.medicalAnalysis.diabetes_type_1) {
      recommendations['diabetes_type_1'] = {
        safetyLevel: parsed.medicalAnalysis.diabetes_type_1.safetyLevel || 'Unknown',
        reasoning: parsed.medicalAnalysis.diabetes_type_1.reasoning || '',
        keyPoints: parsed.medicalAnalysis.diabetes_type_1.keyPoints || [],
        servingAdvice: parsed.medicalAnalysis.diabetes_type_1.servingAdvice || '',
        timingAdvice: parsed.medicalAnalysis.diabetes_type_1.timingAdvice,
        pairingSuggestions: parsed.medicalAnalysis.diabetes_type_1.pairingSuggestions || [],
        alternatives: parsed.medicalAnalysis.diabetes_type_1.alternatives || [],
        bloodSugarImpact: parsed.medicalAnalysis.diabetes_type_1.bloodSugarImpact
      };
    }

    // Map diabetes type 2
    if (parsed.medicalAnalysis.diabetes_type_2) {
      recommendations['diabetes_type_2'] = {
        safetyLevel: parsed.medicalAnalysis.diabetes_type_2.safetyLevel || 'Unknown',
        reasoning: parsed.medicalAnalysis.diabetes_type_2.reasoning || '',
        keyPoints: parsed.medicalAnalysis.diabetes_type_2.keyPoints || [],
        servingAdvice: parsed.medicalAnalysis.diabetes_type_2.servingAdvice || '',
        timingAdvice: parsed.medicalAnalysis.diabetes_type_2.timingAdvice,
        pairingSuggestions: parsed.medicalAnalysis.diabetes_type_2.pairingSuggestions || [],
        alternatives: parsed.medicalAnalysis.diabetes_type_2.alternatives || [],
        bloodSugarImpact: parsed.medicalAnalysis.diabetes_type_2.bloodSugarImpact
      };
    }

    // Map hypertension
    if (parsed.medicalAnalysis.hypertension) {
      recommendations['hypertension'] = {
        safetyLevel: parsed.medicalAnalysis.hypertension.safetyLevel || 'Unknown',
        reasoning: parsed.medicalAnalysis.hypertension.reasoning || '',
        keyPoints: parsed.medicalAnalysis.hypertension.keyPoints || [],
        servingAdvice: parsed.medicalAnalysis.hypertension.servingAdvice || '',
        pairingSuggestions: parsed.medicalAnalysis.hypertension.pairingSuggestions || [],
        alternatives: parsed.medicalAnalysis.hypertension.alternatives || [],
        bloodPressureImpact: parsed.medicalAnalysis.hypertension.bloodPressureImpact
      };
    }

    // Map heart disease
    if (parsed.medicalAnalysis.heart_disease) {
      recommendations['heart_disease'] = {
        safetyLevel: parsed.medicalAnalysis.heart_disease.safetyLevel || 'Unknown',
        reasoning: parsed.medicalAnalysis.heart_disease.reasoning || '',
        keyPoints: parsed.medicalAnalysis.heart_disease.keyPoints || [],
        servingAdvice: parsed.medicalAnalysis.heart_disease.servingAdvice || '',
        pairingSuggestions: parsed.medicalAnalysis.heart_disease.pairingSuggestions || [],
        alternatives: parsed.medicalAnalysis.heart_disease.alternatives || [],
        heartHealthImpact: parsed.medicalAnalysis.heart_disease.heartHealthImpact
      };
    }

    // Transform alternatives
    const primaryAlternatives: Alternative[] = (parsed.alternatives.suggestions || [])
      .slice(0, 4) // Limit to 4 alternatives
      .map((alt: any, index: number) => ({
        foodId: generateFoodId(alt.foodName || `alternative_${index}`),
        advantage: alt.advantage || '',
        improvement: alt.improvement || '',
        bestFor: alt.bestFor || []
      }));

    return {
      id: foodId,
      name: foodName,
      normalizedName: generateNormalizedName(foodName),
      category: parsed.basicInfo.category || 'Unknown',
      calories: parsed.basicInfo.estimatedNutrition?.calories || 0,
      carbs: parsed.basicInfo.estimatedNutrition?.carbs || 0,
      fiber: parsed.basicInfo.estimatedNutrition?.fiber || 0,
      sugars: parsed.basicInfo.estimatedNutrition?.sugars || 0,
      protein: parsed.basicInfo.estimatedNutrition?.protein || 0,
      totalFat: parsed.basicInfo.estimatedNutrition?.totalFat || 0,
      saturatedFat: parsed.basicInfo.estimatedNutrition?.saturatedFat || 0,
      sodium: parsed.basicInfo.estimatedNutrition?.sodium || 0,
      potassium: parsed.basicInfo.estimatedNutrition?.potassium || 0,
      glycemicIndex: parsed.basicInfo.glycemicInfo?.glycemicIndex,
      glycemicLoad: parsed.basicInfo.glycemicInfo?.glycemicLoad,
      recommendations,
      primaryAlternatives,
      alternativeReasoning: parsed.alternatives.reasoning || '',
      glycemicImpact: parsed.basicInfo.glycemicInfo?.glycemicImpact || 'Unknown',
      nutritionalDensity: parsed.nutritionalDensity || 'Medium'
    };

  } catch (error) {
    console.error('Error parsing Gemini response:', error);
    return null;
  }
}

/**
 * Main Cloud Function: Analyze food using Gemini AI
 */
export const geminiFoodAnalysis = functions
  .runWith({
    timeoutSeconds: 120,
    memory: '1GB'
  })
  .https.onCall(async (data: GeminiFoodAnalysisRequest, context) => {
    try {
      // Validate authentication
      if (!context.auth) {
        throw new functions.https.HttpsError(
          'unauthenticated',
          'User must be authenticated to analyze foods'
        );
      }

      const userId = context.auth.uid;

      // Validate input
      if (!data.foodName || !validateFoodName(data.foodName)) {
        throw new functions.https.HttpsError(
          'invalid-argument',
          'Invalid food name provided'
        );
      }

      // Check rate limiting
      if (!checkRateLimit(userId)) {
        throw new functions.https.HttpsError(
          'resource-exhausted',
          'Rate limit exceeded. Please try again later.'
        );
      }

      const foodName = data.foodName.trim();
      const normalizedName = generateNormalizedName(foodName);

      // Check if food already exists in Firestore
      const firestore = admin.firestore();
      const existingFood = await firestore
        .collection('foodItems')
        .where('normalizedName', '==', normalizedName)
        .limit(1)
        .get();

      if (!existingFood.empty) {
        // Return existing food data
        const foodDoc = existingFood.docs[0];
        return {
          success: true,
          data: foodDoc.data(),
          cached: true
        };
      }

      // Call Gemini API (initialize client lazily)
      let client: any;
      try {
        client = getGenAI();
      } catch (e: any) {
        if (e.message === 'GEMINI_API_KEY_MISSING') {
          throw new functions.https.HttpsError(
            'failed-precondition',
            'AI service not configured. Please set GEMINI_API_KEY in functions config or environment.'
          );
        }
        throw e;
      }

      const model = client.getGenerativeModel({ model: 'gemini-2.5-flash-lite' });
      const prompt = createGeminiPrompt(foodName);

      console.log(`Calling Gemini API for food: ${foodName}`);

      // Call Gemini API and robustly extract text from its response.
      let result: any;
      if (typeof model.generateContent === 'function') {
        result = await model.generateContent(prompt);
      } else if (typeof model.generate === 'function') {
        result = await model.generate(prompt);
      } else if (typeof client.generate === 'function') {
        result = await client.generate({model: 'gemini-2.5-flash-lite', prompt});
      } else {
        throw new Error('Unsupported Gemini client API shape');
      }

      // Helper to extract text from various SDK response shapes
      const extractResponseText = async (res: any): Promise<string> => {
        try {
          if (!res) return '';

          // If SDK returns a Response-like object
          if (typeof res.text === 'function') {
            return await res.text();
          }

          // If SDK returns a promise that resolves to an object with .response
          if (res.response && typeof res.response.text === 'function') {
            return await res.response.text();
          }

          // Newer SDKs often return .output or .outputs arrays
          if (Array.isArray(res.output) && res.output.length > 0) {
            // join any text parts
            return res.output.map((o: any) => o.content || o[0]?.text || '').join('\n').trim();
          }

          if (Array.isArray(res.outputs) && res.outputs.length > 0) {
            return res.outputs.map((o: any) => o.content || o[0]?.text || '').join('\n').trim();
          }

          // Fallback to JSON stringify
          return typeof res === 'string' ? res : JSON.stringify(res);
        } catch (e) {
          console.error('Failed extracting text from Gemini response', e);
          return '';
        }
      };

      const geminiResponse = await extractResponseText(result.response || result);

      console.log(`Gemini response received for ${foodName}`);

      // Parse and validate response
      const parsedData = parseGeminiResponse(geminiResponse, foodName);

      if (!parsedData) {
        throw new functions.https.HttpsError(
          'internal',
          'Failed to parse AI response. Please try again.'
        );
      }

      // Save to Firestore
      await firestore
        .collection('foodItems')
        .doc(parsedData.id)
        .set({
          ...parsedData,
          createdAt: admin.firestore.FieldValue.serverTimestamp(),
          createdBy: userId,
          lastUpdated: admin.firestore.FieldValue.serverTimestamp()
        });

      console.log(`Food analysis saved to Firestore: ${parsedData.id}`);

      return {
        success: true,
        data: parsedData,
        cached: false
      };

    } catch (error) {
      console.error('Error in geminiFoodAnalysis:', error);

      if (error instanceof functions.https.HttpsError) {
        throw error;
      }

      // Handle Gemini API errors
      if (error.message?.includes('API_KEY')) {
        throw new functions.https.HttpsError(
          'failed-precondition',
          'AI service configuration error'
        );
      }

      // Handle timeout
      if (error.message?.includes('timeout')) {
        throw new functions.https.HttpsError(
          'deadline-exceeded',
          'Request timed out. Please try again.'
        );
      }

      // Generic error
      throw new functions.https.HttpsError(
        'internal',
        'An unexpected error occurred. Please try again.'
      );
    }
  });
