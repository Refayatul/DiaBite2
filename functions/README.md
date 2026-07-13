# Firebase Cloud Functions for DiaBite

This directory contains Firebase Cloud Functions that provide AI-powered food analysis using Google's Gemini API.

## Functions

### `geminiFoodAnalysis`

Analyzes food items for diabetic and heart patients using Google's Gemini AI.

**Trigger**: HTTPS Callable Function
**Authentication**: Required (Firebase Auth)
**Rate Limit**: 10 requests per hour per user

#### Request Format
```typescript
{
  foodName: string  // Name of the food to analyze
}
```

#### Response Format
```typescript
{
  success: boolean,
  data: FoodItem,    // Complete food analysis data
  cached: boolean    // Whether data came from cache or fresh analysis
}
```

#### Features
- **Duplicate Detection**: Checks Firestore before calling Gemini API
- **Comprehensive Analysis**: Nutritional info, medical recommendations, alternatives
- **Medical Intelligence**: Specific advice for diabetes (type 1 & 2), hypertension, heart disease
- **Smart Alternatives**: 3-4 alternative food suggestions with advantages
- **Rate Limiting**: Prevents API abuse with per-user limits
- **Error Handling**: Comprehensive error handling for API failures, timeouts, invalid responses

## Setup Instructions

### 1. Install Dependencies
```bash
cd functions
npm install
```

### 2. Configure Gemini API Key

**Option A: Environment Variables (Recommended for Production)**
```bash
# Set in Firebase Functions config
firebase functions:config:set gemini.api_key="your_gemini_api_key_here"
```

**Option B: Local Development**
Create a `.env` file in the functions directory:
```
GEMINI_API_KEY=your_gemini_api_key_here
```

### 3. Deploy Functions
```bash
# Deploy to Firebase
firebase deploy --only functions

# Or run locally for development
firebase emulators:start --only functions
```

## Security Features

- **Authentication Required**: All requests must include valid Firebase Auth token
- **Input Validation**: Food names are validated for length and malicious content
- **Rate Limiting**: 10 requests per hour per user to prevent API abuse
- **API Key Protection**: Gemini API key stored securely in environment variables

## Error Handling

The function handles various error scenarios:

- **Authentication Errors**: `unauthenticated` - User not logged in
- **Validation Errors**: `invalid-argument` - Invalid food name
- **Rate Limit Errors**: `resource-exhausted` - Too many requests
- **API Errors**: `internal` - Gemini API failures or parsing errors
- **Timeout Errors**: `deadline-exceeded` - Request took too long

## Data Structure

The function returns food data in the same format as the Android app:

```typescript
interface FoodItem {
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
```

## Gemini AI Prompt

The function uses a comprehensive prompt that instructs Gemini to:

1. Analyze nutritional content per 100g serving
2. Provide medical recommendations for 4 conditions:
   - Diabetes Type 1
   - Diabetes Type 2
   - Hypertension
   - Heart Disease
3. Suggest 3-4 alternative foods with specific advantages
4. Include practical serving advice and preparation tips
5. Focus on evidence-based, medically accurate information

## Development

### Local Testing
```bash
# Start Firebase emulators
firebase emulators:start

# Test the function
curl -X POST http://localhost:5001/diabite-demo/us-central1/geminiFoodAnalysis \
  -H "Content-Type: application/json" \
  -d '{"data": {"foodName": "apple"}}'
```

### Logging
All function calls are logged with relevant information:
- User ID and food name
- API call success/failure
- Processing time
- Error details

## Cost Considerations

- **Gemini API Costs**: Each food analysis costs based on input/output tokens
- **Firestore Costs**: Read/write operations for caching
- **Rate Limiting**: Helps control API usage and costs

## Future Enhancements

- **Batch Processing**: Analyze multiple foods in one request
- **Caching Improvements**: More sophisticated caching strategies
- **Analytics**: Track popular foods and user patterns
- **A/B Testing**: Different prompt versions for optimization
