import { Storage } from '@google-cloud/storage';

const storage = new Storage();
const bucketName = process.env.BUCKET_NAME;

export const acceptTelemetryPost = async (req, res) => {
  if (req.method !== 'POST') {
    return res.status(405).send('Only POST allowed');
  }

  const contentType = req.headers['content-type'] || '';

  // Only allow application/json
  if (!contentType.startsWith('application/json')) {
    return res.status(415).send('Content-Type must be application/json');
  }

  // Cloud Functions v2 keeps rawBody available by default
  const body = req.rawBody;

  if (!body) {
    return res.status(400).send('Missing raw body');
  }

  const timestamp = Date.now();
  const file = storage.bucket(bucketName).file(`post-${timestamp}.txt`);

  try {
    await file.save(body);
    res.status(200).send();
  } catch (err) {
    console.error('GCS write error:', err);
    res.status(500).send('Error writing GCS');
  }
};