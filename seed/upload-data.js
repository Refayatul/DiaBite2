import admin from 'firebase-admin';
import fs from 'fs';
import path from 'path';

// Configuration
const CONFIG = {
  BATCH_SIZE: 500, // Firestore batch limit
  RETRY_ATTEMPTS: 3,
  RETRY_DELAY: 1000 // 1 second
};

// Initialize Firebase Admin
let db;
let serviceAccount;

try {
  serviceAccount = JSON.parse(fs.readFileSync('./serviceAccountKey.json', 'utf8'));
  admin.initializeApp({
    credential: admin.credential.cert(serviceAccount)
  });
  db = admin.firestore();
} catch (error) {
  console.error('❌ Failed to initialize Firebase Admin:', error.message);
  console.error('Make sure serviceAccountKey.json exists and is valid');
  process.exit(1);
}

/**
 * Sleep utility for retry delays
 */
const sleep = (ms) => new Promise(resolve => setTimeout(resolve, ms));

/**
 * Validate foodItems
 */
function validateFoodItems(data) {
  if (!data.foodItems || !Array.isArray(data.foodItems)) {
    throw new Error('Invalid data format: foodItems should be an array');
  }
  data.foodItems.forEach((item, index) => {
    if (!item.id || !item.name) {
      throw new Error(`Missing id or name in foodItems at index ${index}`);
    }
  });
  return true;
}

/**
 * Read JSON file
 */
function readJsonFile(filePath) {
  const fileContent = fs.readFileSync(filePath, 'utf8');
  const data = JSON.parse(fileContent);
  validateFoodItems(data);
  return data;
}

/**
 * Add metadata
 */
function addMetadata(doc) {
  const now = new Date().toISOString();
  return {
    ...doc,
    uploadedAt: now,
    uploadedBy: 'system_upload_script',
    version: 1
  };
}

/**
 * Upload batch
 */
async function uploadBatch(documents) {
  const batch = db.batch();
  documents.forEach(doc => {
    const docRef = db.collection('foodItems').doc(doc.id);
    batch.set(docRef, addMetadata(doc), { merge: true });
  });

  for (let attempt = 1; attempt <= CONFIG.RETRY_ATTEMPTS; attempt++) {
    try {
      await batch.commit();
      return documents.length;
    } catch (error) {
      console.warn(`⚠️  Batch upload attempt ${attempt} failed: ${error.message}`);
      if (attempt === CONFIG.RETRY_ATTEMPTS) throw error;
      await sleep(CONFIG.RETRY_DELAY * attempt);
    }
  }
}

/**
 * Main upload function
 */
async function uploadFoodData(jsonFilePath) {
  console.log('🚀 Starting foodItems upload process...');
  const data = readJsonFile(jsonFilePath);
  const items = data.foodItems;
  console.log(`📦 Total foodItems to upload: ${items.length}`);

  let uploaded = 0;
  for (let i = 0; i < items.length; i += CONFIG.BATCH_SIZE) {
    const batchItems = items.slice(i, i + CONFIG.BATCH_SIZE);
    const count = await uploadBatch(batchItems);
    uploaded += count;
    console.log(`✅ Uploaded batch ${Math.floor(i / CONFIG.BATCH_SIZE) + 1} (${count} items)`);
  }

  console.log(`🎉 Upload completed! Total foodItems uploaded: ${uploaded}`);
}

// Run script
const args = process.argv.slice(2);
const jsonFile = args[0] || './food-data.json';

uploadFoodData(jsonFile).catch(err => {
  console.error('💥 Fatal error:', err.message);
  process.exit(1);
});
