import urllib.request
import json

urls = {
    "qwen3": "https://huggingface.co/litert-community/Qwen3-0.6B/resolve/main/qwen3-0.6b-int4.litertlm?download=true",
    "gemma4": "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it.litertlm?download=true"
}

results = {}
for name, url in urls.items():
    try:
        req = urllib.request.Request(url, method="HEAD")
        with urllib.request.urlopen(req) as response:
            results[name] = response.headers.get('Content-Length')
    except Exception as e:
        results[name] = str(e)

print(json.dumps(results))
