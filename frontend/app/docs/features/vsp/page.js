'use client';
import { useTheme } from '@/components/docs/ThemeProvider';

export default function VSPPage() {
  const { theme } = useTheme();
  const isDark = theme === 'dark';
  const h1 = `text-3xl font-bold mb-4 ${isDark ? 'text-white' : 'text-gray-900'}`;
  const h2 = `text-2xl font-semibold mt-8 mb-3 ${isDark ? 'text-white' : 'text-gray-900'}`;
  const p = `mb-4 leading-relaxed ${isDark ? 'text-gray-300' : 'text-gray-700'}`;
  const code = `${isDark ? 'bg-gray-800 text-green-400' : 'bg-gray-100 text-gray-800'} rounded px-3 py-2 block overflow-x-auto text-sm font-mono my-3 whitespace-pre`;
  const li = `mb-2 ${isDark ? 'text-gray-300' : 'text-gray-700'}`;

  return (
    <div>
      <h1 className={h1}>CVE Vulnerability Score Prediction (VSP)</h1>
      <p className={p}>
        The VSP module uses machine learning to predict CVSS v3.1 scores from natural language vulnerability descriptions. Enter a vulnerability description, and the model predicts the base metrics (Attack Vector, Complexity, Privileges, Impact, etc.) and calculates the CVSS score.
      </p>

      <h2 className={h2}>Architecture</h2>
      <ul className="list-disc pl-6 mb-4">
        <li className={li}><strong>ML Backend:</strong> Python FastAPI on port 5002 (container: vsp-backend)</li>
        <li className={li}><strong>Storage:</strong> MongoDB collection <code>vsp_predictions</code> in <code>container_db</code> via the pentest backend (port 3001)</li>
        <li className={li}><strong>Model:</strong> Trained on NVD CVE data to predict CVSS v3.1 base metrics from text descriptions</li>
      </ul>

      <h2 className={h2}>How It Works</h2>
      <ol className="list-decimal pl-6 mb-4">
        <li className={li}>Enter a vulnerability description (e.g., &quot;A remote code execution vulnerability exists in Apache Log4j...&quot;)</li>
        <li className={li}>Click <strong>Predict</strong> &mdash; the ML model predicts CVSS base metrics</li>
        <li className={li}>Review and adjust the predicted metrics if needed &mdash; the score recalculates in real-time</li>
        <li className={li}>Click <strong>Save</strong> to persist the prediction to MongoDB</li>
      </ol>

      <h2 className={h2}>CVSS v3.1 Metrics</h2>
      <p className={p}>The model predicts all 8 base metrics:</p>
      <ul className="list-disc pl-6 mb-4">
        <li className={li}><strong>Attack Vector (AV):</strong> Network, Adjacent, Local, Physical</li>
        <li className={li}><strong>Attack Complexity (AC):</strong> Low, High</li>
        <li className={li}><strong>Privileges Required (PR):</strong> None, Low, High</li>
        <li className={li}><strong>User Interaction (UI):</strong> None, Required</li>
        <li className={li}><strong>Scope (S):</strong> Unchanged, Changed</li>
        <li className={li}><strong>Confidentiality (C):</strong> None, Low, High</li>
        <li className={li}><strong>Integrity (I):</strong> None, Low, High</li>
        <li className={li}><strong>Availability (A):</strong> None, Low, High</li>
      </ul>

      <h2 className={h2}>Prediction Storage</h2>
      <p className={p}>
        Saved predictions are stored in MongoDB and persist across sessions. The Previous Predictions table shows all saved predictions with CVSS scores, severity ratings, and CVSS vectors. Predictions can be exported as JSON or cleared individually.
      </p>

      <h2 className={h2}>API Endpoints</h2>
      <pre className={code}>{`# Predict CVSS from description (ML backend)
POST http://localhost:5002/predict
Body: { "description": "vulnerability description text" }

# Recalculate score with modified metrics (ML backend)
POST http://localhost:5002/recalculate
Body: { "AV": "NETWORK", "AC": "LOW", ... }

# Save prediction (pentest backend)
POST http://localhost:3001/vsp/predictions
Body: { "description": "...", "vector": "CVSS:3.1/...", "cvss_score": "9.8", "severity": "CRITICAL", ... }

# List all saved predictions
GET http://localhost:3001/vsp/predictions

# Delete a prediction
DELETE http://localhost:3001/vsp/predictions/:id

# Clear all predictions
DELETE http://localhost:3001/vsp/predictions`}</pre>

      <h2 className={h2}>Usage</h2>
      <p className={p}>Navigate to <strong>CTI &rarr; VSP Predictor</strong> in the dashboard.</p>
    </div>
  );
}
