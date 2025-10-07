# Fit4Ever - Architecture Diagrams & Screenshots Guide

## 📐 Architecture Diagram Designs

### Option 1: High-Level System Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         USER / CLIENT                            │
│                    (Web Browser / Mobile)                        │
└───────────────────────────┬─────────────────────────────────────┘
                            │
                            │ HTTPS
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│                    FRONTEND LAYER                                │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  • Vanilla JavaScript (ES6+)                              │  │
│  │  • Responsive HTML5 / CSS3                                │  │
│  │  • JWT Token Management                                   │  │
│  │  • OAuth2 Flow Handling                                   │  │
│  └──────────────────────────────────────────────────────────┘  │
└───────────────────────────┬─────────────────────────────────────┘
                            │
                            │ REST API (JSON)
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│                    SECURITY LAYER                                │
│  ┌─────────────────┐  ┌──────────────────┐  ┌───────────────┐  │
│  │ Rate Limiting   │  │  JWT Auth        │  │  OAuth2       │  │
│  │ Filter          │→ │  Filter          │→ │  Integration  │  │
│  │ (Caffeine)      │  │  (Validation)    │  │  (Google)     │  │
│  └─────────────────┘  └──────────────────┘  └───────────────┘  │
└───────────────────────────┬─────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│               BACKEND LAYER (Spring Boot 3.5.5)                  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │                    CONTROLLER LAYER                       │  │
│  │  • AuthController     • WorkoutController                 │  │
│  │  • UserController     • NutritionController               │  │
│  │  • GoalController     • OAuth2Controller                  │  │
│  └────────────────────────┬─────────────────────────────────┘  │
│                            │                                     │
│  ┌────────────────────────▼─────────────────────────────────┐  │
│  │                    SERVICE LAYER                          │  │
│  │  • AuthService        • WorkoutService                    │  │
│  │  • GoalService        • NutritionService                  │  │
│  │  • OAuth2UserService  • CustomUserDetailsService          │  │
│  └────────────────────────┬─────────────────────────────────┘  │
│                            │                                     │
│  ┌────────────────────────▼─────────────────────────────────┐  │
│  │                  REPOSITORY LAYER (JPA)                   │  │
│  │  • UserRepository     • WorkoutRepository                 │  │
│  │  • GoalRepository     • FoodLogRepository                 │  │
│  └────────────────────────┬─────────────────────────────────┘  │
└────────────────────────────┼─────────────────────────────────────┘
                             │
                             │ JDBC / Hibernate
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                    DATABASE LAYER                                │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  Development: H2 In-Memory Database                       │  │
│  │  Production:  PostgreSQL                                  │  │
│  │                                                            │  │
│  │  Tables: users, workouts, exercises, food_logs, goals     │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

---

### Option 2: Security & Authentication Flow

```
┌──────────────┐
│   Client     │
└──────┬───────┘
       │
       │ 1. POST /api/auth/login
       │    (email, password)
       ▼
┌─────────────────────────────────────────────────────────┐
│              Rate Limiting Filter                       │
│  • Check IP + endpoint                                  │
│  • Caffeine Cache (1 min TTL)                          │
│  • Max 10 requests/min                                  │
└──────────────────┬──────────────────────────────────────┘
                   │ ✓ Pass
                   ▼
┌─────────────────────────────────────────────────────────┐
│              Auth Controller                            │
│  • Validate credentials                                 │
│  • BCrypt password check                                │
└──────────────────┬──────────────────────────────────────┘
                   │ ✓ Valid
                   ▼
┌─────────────────────────────────────────────────────────┐
│              JWT Util                                   │
│  • Generate token                                       │
│  • Sign with HMAC-SHA256                               │
│  • Set expiration (1 hour)                             │
└──────────────────┬──────────────────────────────────────┘
                   │
                   │ 2. Response: { "token": "eyJ..." }
                   ▼
┌──────────────┐
│   Client     │ (stores JWT in localStorage)
└──────┬───────┘
       │
       │ 3. GET /api/workouts
       │    Header: Authorization: Bearer eyJ...
       ▼
┌─────────────────────────────────────────────────────────┐
│              JWT Auth Filter                            │
│  • Extract token from header                            │
│  • Validate signature                                   │
│  • Check expiration                                     │
│  • Load user details                                    │
└──────────────────┬──────────────────────────────────────┘
                   │ ✓ Valid JWT
                   ▼
┌─────────────────────────────────────────────────────────┐
│              Protected Controller                       │
│  • Access user from SecurityContext                     │
│  • Execute business logic                               │
│  • Return user-specific data                            │
└─────────────────────────────────────────────────────────┘


OAUTH2 FLOW:
┌──────────────┐
│   Client     │
└──────┬───────┘
       │ 1. Click "Sign in with Google"
       ▼
┌─────────────────────────────────────────────────────────┐
│         Redirect to Google OAuth2                       │
└──────────────────┬──────────────────────────────────────┘
                   │
                   │ 2. User authenticates with Google
                   ▼
┌─────────────────────────────────────────────────────────┐
│         Google redirects back to app                    │
│         /login/oauth2/code/google?code=xyz              │
└──────────────────┬──────────────────────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────────────────────┐
│         OAuth2UserService                               │
│  • Load user info from Google                           │
│  • Create/update user in database                       │
└──────────────────┬──────────────────────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────────────────────┐
│         OAuth2AuthenticationSuccessHandler              │
│  • Generate JWT token                                   │
│  • Redirect to /oauth2/redirect?token=eyJ...           │
└─────────────────────────────────────────────────────────┘
```

---

### Option 3: Data Flow Architecture

```
┌────────────────────────────────────────────────────────────────┐
│                    REQUEST FLOW                                 │
└────────────────────────────────────────────────────────────────┘

   Client Request
        │
        ▼
   ┌─────────────────┐
   │ RateLimiting    │ ← Caffeine Cache (in-memory)
   │ Filter          │
   └────────┬────────┘
            │
            ▼
   ┌─────────────────┐
   │ JWT Auth        │ ← JWT Util (token validation)
   │ Filter          │
   └────────┬────────┘
            │
            ▼
   ┌─────────────────┐
   │ Controller      │ ← Input Validation (Bean Validation)
   │ (REST API)      │
   └────────┬────────┘
            │
            ▼
   ┌─────────────────┐
   │ Service         │ ← Business Logic
   │ Layer           │
   └────────┬────────┘
            │
            ▼
   ┌─────────────────┐
   │ Repository      │ ← JPA / Hibernate
   │ Layer           │
   └────────┬────────┘
            │
            ▼
   ┌─────────────────┐
   │ Database        │
   │ (PostgreSQL)    │
   └─────────────────┘


┌────────────────────────────────────────────────────────────────┐
│                    ENTITY RELATIONSHIPS                         │
└────────────────────────────────────────────────────────────────┘

                    ┌──────────────┐
                    │     USER     │
                    │              │
                    │ - id         │
                    │ - email      │
                    │ - password   │
                    │ - name       │
                    │ - provider   │
                    └──────┬───────┘
                           │
            ┌──────────────┼──────────────┐
            │              │              │
            ▼              ▼              ▼
    ┌──────────┐   ┌──────────┐   ┌──────────┐
    │ WORKOUT  │   │ FOOD_LOG │   │   GOAL   │
    │          │   │          │   │          │
    │ - id     │   │ - id     │   │ - id     │
    │ - userId │   │ - userId │   │ - userId │
    │ - date   │   │ - date   │   │ - type   │
    │ - notes  │   │ - meal   │   │ - target │
    └────┬─────┘   │ - cals   │   │ - current│
         │         └──────────┘   │ - status │
         │                        └──────────┘
         │
         │ One-to-Many
         ▼
    ┌──────────┐
    │ EXERCISE │
    │          │
    │ - id     │
    │ - workout│
    │ - name   │
    │ - sets   │
    │ - reps   │
    │ - weight │
    └──────────┘
```

---

### Option 4: Deployment Architecture

```
┌────────────────────────────────────────────────────────────────┐
│                     DEPLOYMENT FLOW                             │
└────────────────────────────────────────────────────────────────┘

   Developer
       │
       │ git push
       ▼
   ┌─────────────┐
   │   GitHub    │
   │ Repository  │
   └──────┬──────┘
          │
          │ webhook / manual deploy
          ▼
   ┌─────────────────────────────────────────┐
   │        BUILD PROCESS                    │
   │                                         │
   │  1. Maven Build (mvn clean package)    │
   │  2. Run Tests (JUnit)                  │
   │  3. Create JAR file                     │
   │  4. Docker Build (multi-stage)          │
   │  5. Push to Container Registry          │
   └──────────────┬──────────────────────────┘
                  │
     ┌────────────┴────────────┐
     │                         │
     ▼                         ▼
┌─────────────┐         ┌─────────────┐
│   Railway   │         │   AWS ECS   │
│             │         │             │
│ • Auto      │         │ • Task Def  │
│   deploy    │         │ • Container │
│ • ENV vars  │         │ • Load Bal  │
└─────────────┘         └─────────────┘


┌────────────────────────────────────────────────────────────────┐
│                  DOCKER ARCHITECTURE                            │
└────────────────────────────────────────────────────────────────┘

   ┌─────────────────────────────────────────────┐
   │         Docker Container                    │
   │                                             │
   │  ┌───────────────────────────────────────┐ │
   │  │  OpenJDK 17 (Alpine Linux)            │ │
   │  └───────────────────────────────────────┘ │
   │                                             │
   │  ┌───────────────────────────────────────┐ │
   │  │  Spring Boot Application              │ │
   │  │  (fit4ever-0.0.1-SNAPSHOT.jar)        │ │
   │  │                                       │ │
   │  │  Port: 8080                           │ │
   │  │  User: nonroot (security)             │ │
   │  └───────────────────────────────────────┘ │
   │                                             │
   │  ┌───────────────────────────────────────┐ │
   │  │  Health Check Endpoint                │ │
   │  │  /actuator/health                     │ │
   │  └───────────────────────────────────────┘ │
   └─────────────────────────────────────────────┘
                      │
                      │ connects to
                      ▼
   ┌─────────────────────────────────────────────┐
   │         PostgreSQL Database                 │
   │         (External Service)                  │
   └─────────────────────────────────────────────┘
```

---

## 🖼️ How to Create These Diagrams

### Option A: Draw.io (Recommended - Free & Easy)

1. Go to https://app.diagrams.net/
2. Choose "Create New Diagram"
3. Use these shapes:
   - **Rectangles** for components
   - **Cylinders** for databases
   - **Arrows** for data flow
   - **Cloud shapes** for external services

**Colors to Use:**
- Frontend: Light Blue (#E3F2FD)
- Security: Orange (#FFE0B2)
- Backend: Green (#C8E6C9)
- Database: Purple (#E1BEE7)
- External: Yellow (#FFF9C4)

### Option B: Excalidraw (Hand-drawn style)

1. Go to https://excalidraw.com/
2. Drag and drop rectangles
3. Add arrows and text
4. Export as PNG

### Option C: Lucidchart (Professional)

1. Go to https://www.lucidchart.com/
2. Use AWS/Cloud architecture templates
3. Export high-quality PNG

### Option D: ASCII Art (Quick & Simple)

Just copy the ASCII diagrams above and take a screenshot!

---

## 📸 How to Take Swagger API Screenshot

### Step-by-Step Instructions:

#### 1. Start Your Application

```bash
# Navigate to your project directory
cd /Users/prakashrajkalidoss/Documents/PersonalDocs/fit4ever

# Start the application
mvn spring-boot:run

# Wait for the message:
# "Started Fit4EverApplication in X.XXX seconds"
```

#### 2. Open Swagger UI in Browser

```
http://localhost:8080/swagger-ui.html
```

or try:

```
http://localhost:8080/swagger-ui/index.html
```

#### 3. What You'll See

The Swagger UI will show all your API endpoints organized by controllers:

- **auth-controller** (Authentication endpoints)
- **goal-controller** (Goal management)
- **nutrition-controller** (Food logging)
- **o-auth-2-controller** (OAuth2 endpoints)
- **user-controller** (User management)
- **workout-controller** (Workout tracking)

#### 4. Best Screenshots to Take

**Screenshot 1: Overview Page**
- Shows all controller groups collapsed
- Clean, professional look
- Captures the full API structure

**Screenshot 2: Expanded Auth Controller**
- Click on "auth-controller" to expand
- Shows: POST /api/auth/login, POST /api/auth/register
- Great for showing authentication endpoints

**Screenshot 3: Single Endpoint Details**
- Click on any endpoint (e.g., POST /api/workouts)
- Click "Try it out"
- Shows request body schema, response codes
- Demonstrates API documentation quality

**Screenshot 4: Schema Models**
- Scroll down to "Schemas" section at bottom
- Shows your DTOs (LoginRequest, WorkoutRequest, etc.)
- Demonstrates data modeling

#### 5. Taking the Screenshot (Mac)

**Full Page Screenshot:**
```
⌘ + Shift + 3  (Entire screen)
⌘ + Shift + 4  (Select region)
⌘ + Shift + 5  (Screenshot tool with options)
```

**Pro Tips:**
- Use `⌘ + Shift + 4` then press `Space` to screenshot just the browser window
- Hide bookmarks bar for cleaner look
- Zoom to 100% or 110% for clarity
- Use incognito mode to avoid extensions

#### 6. Browser Extensions for Better Screenshots

**For Chrome/Edge:**
- "Awesome Screenshot" - Scrolling capture
- "GoFullPage" - Full page screenshot

**For Firefox:**
- Built-in screenshot tool (Right-click → Take Screenshot)

#### 7. Test an Endpoint (Optional but Impressive)

To show your API working:

1. Click on POST `/api/auth/register`
2. Click "Try it out"
3. Enter test data:
```json
{
  "name": "Test User",
  "email": "test@example.com",
  "password": "Test123!"
}
```
4. Click "Execute"
5. Screenshot the successful 200 response

---

## 📱 Screenshot Best Practices

### For LinkedIn Posts:

1. **Resolution**: 1200x627px (optimal LinkedIn image size)
2. **Format**: PNG for clarity, JPG for smaller file size
3. **Annotation**: Use tools like:
   - macOS Preview (markup tools)
   - Snagit
   - Skitch
   - Photoshop / Figma

### What to Highlight:

- ✅ Add arrows pointing to key features
- ✅ Circle important endpoints
- ✅ Add text boxes explaining architecture
- ✅ Use consistent colors
- ✅ Keep text readable (min 14pt font)

### Tools for Annotation:

**Free:**
- Preview (Mac built-in)
- Paint.NET (Windows)
- GIMP

**Paid:**
- Snagit ($50) - Best for screenshots
- Camtasia - If you want to record video
- Adobe Photoshop

---

## 🎨 Color Scheme Recommendations

For professional architecture diagrams:

```
Primary Colors:
- Frontend:    #3498DB (Blue)
- Backend:     #2ECC71 (Green)  
- Database:    #9B59B6 (Purple)
- Security:    #E74C3C (Red/Orange)
- External:    #F39C12 (Orange)

Background:    #FFFFFF (White)
Text:          #2C3E50 (Dark Blue)
Borders:       #BDC3C7 (Light Gray)
Arrows:        #34495E (Dark Gray)
```

---

## 📋 Quick Checklist

Before posting to LinkedIn:

- [ ] Application is running
- [ ] Swagger UI is accessible
- [ ] Screenshots are clear and readable
- [ ] Architecture diagram is created
- [ ] Images are annotated (if needed)
- [ ] File sizes are optimized (<5MB each)
- [ ] Images are in PNG or JPG format
- [ ] You have 2-3 compelling images ready

---

## 🎯 Pro Tips

1. **Create a Carousel**: LinkedIn allows up to 10 images
   - Slide 1: Architecture overview
   - Slide 2: Security flow
   - Slide 3: Swagger API docs
   - Slide 4: Code snippet
   - Slide 5: Tech stack summary

2. **Add Branding**: Include your name/logo in corner

3. **High Contrast**: Ensure text is readable on mobile

4. **Tell a Story**: Each image should build on the previous

5. **Keep It Simple**: Don't overcrowd with information

---

## 🚀 Ready to Create!

You now have everything you need to create professional architecture diagrams and Swagger screenshots for your LinkedIn post!

Need help with any specific step? Let me know!

