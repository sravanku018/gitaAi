#!/usr/bin/env python3
import http.server
import socketserver
import json

PORT = 5000

class MockServerHandler(http.server.SimpleHTTPRequestHandler):
    def do_POST(self):
        content_length = int(self.headers['Content-Length'])
        post_data = self.rfile.read(content_length)
        
        print("\n" + "="*50)
        print(f"Received POST request to: {self.path}")
        print("Headers:")
        for key, value in self.headers.items():
            print(f"  {key}: {value}")
        
        print("\nBody:")
        try:
            json_data = json.loads(post_data.decode('utf-8'))
            print(json.dumps(json_data, indent=4))
        except Exception:
            print(post_data.decode('utf-8'))
            
        print("="*50 + "\n")
        
        # Determine what response to send
        self.send_response(200)
        self.send_header('Content-type', 'application/json')
        self.end_headers()
        
        if self.path.endswith('/quiz/attempt'):
            response = {"status": "success", "id": 999}
        elif self.path.endswith('/api/v1/user/coins/award'):
            response = {"success": True, "coins_awarded": 50, "total_coins": 150, "transaction_id": "tx_mock_123"}
        else:
            response = {"status": "ok"}
            
        self.wfile.write(json.dumps(response).encode('utf-8'))

with socketserver.TCPServer(("", PORT), MockServerHandler) as httpd:
    print(f"Mock server running on port {PORT}...")
    print("Point your Android app's COIN_API_BASE_URL to http://<your-local-ip>:5000/")
    httpd.serve_forever()
