# Google OAuth2 Setup Guide for Fit4Ever

## 🚀 **Quick Setup Steps**

### **Step 1: Google Cloud Console Setup**

1. **Visit**: https://console.cloud.google.com/
2. **Create/Select Project**: Create a new project or select existing one
3. **Enable APIs** (Optional - OAuth2 works without enabling specific APIs):
   - Go to "APIs & Services" > "Library"
   - Search and enable "**People API**" (for profile information)
   - **Note**: Google+ API is deprecated. OAuth2 login works automatically without additional APIs!

### **Step 2: OAuth Consent Screen**

1. **Go to**: "APIs & Services" > "OAuth consent screen"
2. **User Type**: External
3. **App Information**:
   - App name: `Fit4Ever`
   - User support email: `your-email@gmail.com`
   - Developer contact: `your-email@gmail.com`
4. **Scopes**: Add these scopes:
   - `openid`
   - `profile` 
   - `email`
5. **Test Users**: Add your email for testing

### **Step 3: Create OAuth2 Credentials**

1. **Go to**: "APIs & Services" > "Credentials"
2. **Click**: "Create Credentials" > "OAuth 2.0 Client IDs"
3. **Application Type**: Web application
4. **Name**: `Fit4Ever Web Client`
5. **Authorized Redirect URIs**: Add these URLs:
   ```
   http://fit4ever-alb-828924943.us-east-1.elb.amazonaws.com/login/oauth2/code/google
   http://localhost:8080/login/oauth2/code/google
   ```

### **Step 4: Get Your Credentials**

After creating the OAuth2 client, you'll get:
- **Client ID**: `1234567890-abcdefghijklmnop.apps.googleusercontent.com`
- **Client Secret**: `GOCSPX-abcdefghijklmnopqrstuvwxyz`

### **Step 5: Update AWS ECS Task Definition**

Run these commands to update your deployment:

```bash
# 1. Replace YOUR_GOOGLE_CLIENT_ID and YOUR_GOOGLE_CLIENT_SECRET with actual values
sed -i 's/YOUR_GOOGLE_CLIENT_ID/1234567890-abcdefghijklmnop.apps.googleusercontent.com/g' /tmp/oauth2-task-def.json
sed -i 's/YOUR_GOOGLE_CLIENT_SECRET/GOCSPX-abcdefghijklmnopqrstuvwxyz/g' /tmp/oauth2-task-def.json

# 2. Register new task definition
aws ecs register-task-definition --cli-input-json file:///tmp/oauth2-task-def.json

# 3. Update ECS service
aws ecs update-service --cluster fit4ever-cluster --service fit4ever-service --task-definition fit4ever-task:7 --force-new-deployment
```

### **Step 6: Test OAuth2 Flow**

1. **Wait** for deployment to complete (2-3 minutes)
2. **Visit**: http://fit4ever-alb-828924943.us-east-1.elb.amazonaws.com/
3. **Click**: "Continue with Google" button
4. **Verify**: You should be redirected to Google login

## 🔧 **Troubleshooting**

### **Common Issues:**

1. **400 Bad Request**: 
   - Check redirect URIs match exactly
   - Ensure OAuth consent screen is configured

2. **403 Forbidden**:
   - Add your email to test users
   - Verify app is not in production mode

3. **Redirect URI Mismatch**:
   - Double-check the redirect URI in Google Console
   - Ensure it matches: `http://your-domain/login/oauth2/code/google`

### **Security Notes:**

- Keep Client Secret secure
- Use environment variables in production
- Consider using Google Cloud Secret Manager for production

## 📋 **Next Steps After Setup**

1. **Test the OAuth2 flow**
2. **Verify user creation in database**
3. **Test JWT token generation**
4. **Validate session management**

---

**Need Help?** 
- Check Google Cloud Console logs
- Review ECS task logs: `aws logs get-log-events --log-group-name /ecs/fit4ever-task`
