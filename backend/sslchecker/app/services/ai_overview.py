import json
from openai import OpenAI

def get_deepseek_overview(question, host_data, api_key):
    try:
        client = OpenAI(api_key=api_key, base_url="https://api.deepseek.com")

        response = client.chat.completions.create(
            model="deepseek-chat",
            messages=[
                {"role": "system", "content": "I am your cybersecurity assistant"},
                {"role": "user", "content": "Having these server data in json format extracted using various security tools\n" + json.dumps(host_data,  default=str) + "\n can you answer this question considering the user does not have cybersecurity expertise ? If many data appear as None it means the tools could not retrieve information due to issues like SSLv3 or expired certificate. Question:" + question}
            ],
            stream=False
        )

        response = response.choices[0].message.content
        return {
            "success": True,
            "response": response,
        }
    except Exception as e:
        return {
            "success": False,
            "response": str(e),
        }


def analyze_with_deepseek(headers, cert, cipher, heartbleed, api_key):
    try:
        client = OpenAI(api_key=api_key, base_url="https://api.deepseek.com")

        response = client.chat.completions.create(
            model="deepseek-chat",
            messages=[
                {"role": "system", "content": "I am your cybersecurity assistant"},
                {"role": "user", "content": f"""
                    You are a cybersecurity analyst. Given the following server security information (in JSON format), perform a detailed yet simple-to-understand analysis of potential vulnerabilities and misconfigurations. The audience does not have a strong background in cybersecurity, so keep explanations clear, concise, and jargon-free where possible.
                    
                    Focus your analysis on:
                    1. SSL/TLS Configuration – Are the protocols and ciphers strong and modern?
                    2. Certificate Validity – Is the certificate valid and trustworthy?
                    3. HTTP Headers – Are the right security headers present and properly configured?
                    4. Server Software and Known Vulnerabilities – Is any outdated or vulnerable software detected?
                    5. General Security Hygiene – Any obvious risks or poor practices?
                    
                    Please:
                    - Explain what each finding means in plain language.
                    - Highlight any critical risks or attack vectors (e.g., MITM attacks, weak encryption, missing headers).
                    - Suggest clear, prioritized, and actionable steps to fix or mitigate each issue.
                    
                    Here are the data collected from the server in json format:
                    - Certificate Info:
                    {json.dumps(cert, default=str, indent=2)}
                    
                    - HTTP Headers:
                    {json.dumps(headers, default=str, indent=2)}
                    
                    - TLS/SSL Cipher & Protocol Info:
                    {json.dumps(cipher, default=str, indent=2)}
                    
                    - Heartbleed Test Result:
                    {json.dumps(heartbleed, default=str, indent=2)}
                    
                    Provide only the objective security report based on the given data and do not include conversational phrases.
                    """}
            ],
            stream=False
        )
        response = response.choices[0].message.content

        return {
            "success": True,
            "response": response,
        }
    except Exception as e:
        return {
            "success": False,
            "response": str(e),
        }