# Deploying Divisha WhatsApp Bot to Render

This guide will help you deploy your WhatsApp bot application to Render using Docker.

## Prerequisites

1. A Render account (sign up at https://render.com)
2. Your code pushed to a Git repository (GitHub, GitLab, or Bitbucket)

**Note:** All credentials are already configured in `application.properties`, so you don't need to set environment variables manually.

## Deployment Steps (Simple - 3 Steps!)

### Step 1: Push your code to a Git repository

If you haven't already:

```bash
git add .
git commit -m "Prepare for Render deployment"
git push origin master
```

### Step 2: Create a new Web Service on Render

1. Go to https://dashboard.render.com
2. Click "New +" and select "Blueprint"
3. Connect your Git repository
4. Select the repository containing your code
5. Render will automatically detect the `render.yaml` file
6. Click "Apply"

That's it! Render will automatically:
- Build your Docker image
- Use your hardcoded credentials from `application.properties`
- Deploy your application

### Step 3: Wait for deployment

- The first build will take 5-10 minutes (Maven build + frontend compilation)
- Once complete, your app will be available at: `https://divisha-whatsapp-bot.onrender.com`
- The dashboard will be at: `https://divisha-whatsapp-bot.onrender.com/`

## Post-Deployment

### 1. Verify the Deployment

After deployment, verify your application is running:

```bash
curl https://divisha-whatsapp-bot.onrender.com/actuator/health
```

Expected response:
```json
{"status":"UP"}
```

### 2. Configure WhatsApp Webhook

Update your Meta WhatsApp webhook URL to point to your Render deployment:

```
Webhook URL: https://divisha-whatsapp-bot.onrender.com/webhook
Verify Token: test
```

(This uses the value from your `application.properties`: `META_WEBHOOK_VERIFY_TOKEN=test`)

### 3. Test Your Application

- Visit `https://divisha-whatsapp-bot.onrender.com/` to access the dashboard
- Send a WhatsApp message to your configured number to test the bot

## Important Notes

### Free Tier Limitations

- Render's free tier spins down after 15 minutes of inactivity
- The first request after spin-down may take 30-60 seconds to respond
- For production use, consider upgrading to a paid plan ($7/month)

### Build Time

- First deployment: 5-10 minutes (Maven build + frontend compilation)
- Subsequent deployments: Faster thanks to Docker layer caching

### Security Warning

Your credentials are hardcoded in `application.properties`. This is convenient but:
- Anyone with access to your repository can see your credentials
- You'll need to rebuild and redeploy to rotate credentials
- For production, consider using environment variables instead

## Troubleshooting

### Build Failures

If the build fails:
- Check the logs in Render dashboard
- Look for Maven or Node.js errors
- Verify your Dockerfile is correct

### Application Crashes

If the app crashes after deployment:
- Check the logs in Render dashboard
- Verify MongoDB connection (make sure MongoDB Atlas allows connections from 0.0.0.0/0)
- Ensure all required dependencies are available

### Webhook Issues

If webhooks aren't working:
- Verify webhook URL in Meta dashboard: `https://divisha-whatsapp-bot.onrender.com/webhook`
- Verify token matches: `test`
- If using free tier, service may be sleeping (first request takes 30-60 seconds)

## Updating Your Application

To deploy updates:

```bash
git add .
git commit -m "Your update message"
git push origin master
```

Render will automatically detect the push and redeploy your application.

## Useful Links

- Render Dashboard: https://dashboard.render.com
- Render Logs: Check your service in the dashboard
- Meta WhatsApp API Docs: https://developers.facebook.com/docs/whatsapp

## Cost

- **Render Free Tier**: $0/month (sleeps after 15 min inactivity)
- **Render Starter**: $7/month (always on, better performance)
