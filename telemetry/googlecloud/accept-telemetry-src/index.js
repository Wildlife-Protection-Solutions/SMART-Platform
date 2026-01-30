import express from 'express';
import { Storage } from '@google-cloud/storage';

const app = express();
const storage = new Storage();
const bucketName = process.env.BUCKET_NAME;

// --- SECURITY LAYER ---
// This prevents the "Crash" by rejecting large files before parsing.
// 'limit' sets the max body size (e.g., 1MB).
// 'type' ensures we only accept json headers (rejects others automatically or ignores them).
app.use(express.raw({
  type: 'application/json',
  limit: '500kb'
}));

app.post('/', async (req, res) => {
  // 1. Content-Type Check (Redundant due to express.raw type check, but good for custom error msgs)
  const contentType = req.headers['content-type'] || '';
  if (!contentType.startsWith('application/json')) {
    return res.status(415).send('Content-Type must be application/json');
  }

  // 2. Get the Buffer
  // In Cloud Run with express.raw(), req.body IS the raw buffer.
  const bodyBuffer = req.body;

  if (!bodyBuffer || bodyBuffer.length === 0) {
    return res.status(400).send('Missing body');
  }

  // 3. Basic Validation
  // We check if it is valid JSON, but we don't keep the parsed object to save memory.
  try {
    let payload = JSON.parse(bodyBuffer.toString());

    const requiredKeys = ['installkey', 'db_version', 'stats', 'smart.version']; // Your required fields
    const incomingKeys = new Set(Object.keys(payload).map(k => k.toLowerCase()));
    const missingKeys = requiredKeys.filter(reqKey => !incomingKeys.has(reqKey));
    if (missingKeys.length > 0) {
      return res.status(400).send('Invalid JSON format');
    }

  } catch (e) {
    return res.status(400).send('Invalid JSON format');
  }

  // 4. Write to Storage
  const timestamp = Date.now();
  const file = storage.bucket(bucketName).file(`post-${timestamp}.txt`);

  try {
    await file.save(bodyBuffer);
    res.status(200).send('File saved');
  } catch (err) {
    console.error('GCS write error:', err);
    res.status(500).send('Error writing GCS');
  }
});

// Start the server
const port = process.env.PORT || 8080;
app.listen(port, () => {
  console.log(`Telemetry acceptor listening on port ${port}`);
});
