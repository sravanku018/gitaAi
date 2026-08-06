# Testing Mobile-to-Server Sync

This document provides instructions on how to manually verify that the background quiz sync properly sends requests to the server.

## Overview
By default, the Android app sends quiz attempts and coin awards to production endpoints. For testing, we can spin up a local Python mock server on your computer, point the Android app to this local server, and watch the JSON requests come in live.

## Prerequisites
- A device or emulator running on the same network as your computer.
- Python 3 installed on your computer.

## Step 1: Find your Local IP
Find your computer's local network IP address (e.g., `192.168.1.150`).
- **Mac/Linux:** run `ifconfig` or `ipconfig` in the terminal.
- **Windows:** run `ipconfig` in Command Prompt.

## Step 2: Start the Mock Server
In a terminal at the root of the project, run:
```bash
python3 tools/mock_server.py
```
This will start a server listening on port 5000.

## Step 3: Configure Android App to use the Mock Server
1. Open `local.properties` (or `gradle.properties`, depending on where you store your `COIN_API_BASE_URL` secret).
2. Change the API base URL to point to your Python server:
   ```properties
   COIN_API_BASE_URL="http://192.168.1.150:5000/"
   ```
   *(Make sure to use HTTP, not HTTPS, and include the trailing slash).*
3. Also, ensure the network security config allows cleartext traffic for local testing. If you are using Android 9+ (API 28+), you might need to add `android:usesCleartextTraffic="true"` to your `<application>` tag in `AndroidManifest.xml` during testing.

## Step 4: Run the App and Trigger Sync
1. Run the app on your emulator or physical device.
2. Complete a quiz section in the app.
3. Observe the python terminal. You will see the incoming POST requests for:
   - `/quiz/attempt`
   - `/api/v1/user/coins/award`

The mock server will automatically respond with HTTP 200 and a mock payload to satisfy the app, proving that the mobile-to-server connection is functional.
