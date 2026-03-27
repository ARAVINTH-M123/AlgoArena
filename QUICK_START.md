# Quick Start Guide - LeetCode-Style Interface

## Prerequisites
- Java 21+ (for backend)
- Node.js 18+ (for frontend)
- Maven 3.8+ (for building backend)

## Backend Setup

```bash
cd backend

# Compile the new code
./mvnw clean compile

# Run the backend server
./mvnw spring-boot:run
# Or: ./mvnw.cmd spring-boot:run (on Windows)

# Server should start on: http://localhost:8080
```

## Frontend Setup

```bash
cd frontend

# Install dependencies (if not already installed)
npm install

# Run the development server
npm run dev

# Open in browser: http://localhost:3000
```

## Testing the New Interface

### Step 1: Navigate to Problem
- Go to `/problems` page
- Click on any problem from the list

### Step 2: Solve Page Layout
You should see:
- **Left**: Code editor with Java template
- **Vertical divider**: Between code and problem sections (draggable)
- **Right top**: Problem description with sample I/O
- **Horizontal divider**: Between problem description and test cases (draggable)
- **Right bottom**: Custom test input area

### Step 3: Test Custom Test Cases
1. Click in the **Custom Input** text area (right bottom)
2. Enter your test input (e.g., a sample input from the problem)
3. Click the **Run** button
4. See the output appear below

### Step 4: Submit Solution
1. Verify your code is correct
2. Click **Submit Java** button
3. A modal will show the verdict (ACCEPTED, WRONG_ANSWER, etc.)

### Step 5: Resize Panels
- **Vertical Divider**: Drag horizontally to resize code vs. problem panels
- **Horizontal Divider**: Drag vertically to resize problem vs. test cases panels
- Dividers highlight when hovered showing they're interactive

## Expected Features

### Code Editor (Left Panel)
- ✅ Syntax highlighting for Java
- ✅ Line numbers
- ✅ Full Monaco editor features
- ✅ Auto-save to state

### Problem Description (Right Top)
- ✅ Displays problem title, difficulty, tags
- ✅ Full problem description with markdown formatting
- ✅ Input/Output format explanation
- ✅ Constraints
- ✅ Sample I/O test cases
- ✅ Scrollable if content is long

### Custom Test Cases (Right Bottom)
- ✅ Text area for custom input
- ✅ Run button for execution
- ✅ Displays output or errors
- ✅ Shows execution time
- ✅ History of multiple test runs
- ✅ Latest tests appear first

## Troubleshooting

### "Compilation timed out" or "Execution timed out"
- **Cause**: Your code might have an infinite loop or syntax error
- **Fix**: Check your code for infinite loops and syntax errors

### "Runtime error"
- **Cause**: Your program crashed during execution
- **Fix**: Check for null pointer exceptions, array out of bounds, etc.

### Backend not responding
- **Check**: Is the backend running on `http://localhost:8080`?
- **Fix**: Restart the backend with `./mvnw spring-boot:run`

### Frontend shows loading but problem doesn't load
- **Check**: Browser console for errors
- **Fix**: Ensure authentication token is valid and api base URL is correct

### Dividers not dragging smoothly
- **Check**: No other overlapping elements preventing mouse events
- **Fix**: Try a clean refresh with `Ctrl+Shift+R` or `Cmd+Shift+R`

## Building for Production

### Backend
```bash
cd backend
./mvnw clean package
# This creates: target/platform-0.0.1-SNAPSHOT.jar
```

### Frontend
```bash
cd frontend
npm run build
npm run start
```

## Environment Configuration

### Backend (application.yaml)
- Ensure database is configured for storing problems and submissions
- Default runs on `http://localhost:8080`

### Frontend (.env.local)
```
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080/api
```

## File Cleanup

Custom test execution creates temporary directories in the system temp folder:
- **Location**: `{system-temp}/algoarena-executions/`
- **Auto-cleanup**: Yes (cleaned after each execution)
- **Manual cleanup**: Temp files are removed via finally block

## Performance Tips

1. **Drag Dividers Smoothly**: Panels resize in real-time using CSS percentages
2. **Large Code Files**: The Monaco editor can handle large files efficiently
3. **Multiple Tests**: Test results are stored in memory, limit to recent 50 tests
4. **Timeout Settings**:
   - Compilation: 10 seconds
   - Execution: 5 seconds

## Next Steps

1. ✅ Start the backend
2. ✅ Start the frontend
3. ✅ Navigate to `/problems`
4. ✅ Click a problem to solve
5. ✅ Try the new LeetCode-style interface!

## Support

For issues or questions:
1. Check the backend console for Java compilation/execution errors
2. Check the browser console for TypeScript/JavaScript errors
3. Verify API responses in the Network tab
4. Check security configurations in the SecurityConfig.java file

Enjoy your new problem-solving interface! 🎉
