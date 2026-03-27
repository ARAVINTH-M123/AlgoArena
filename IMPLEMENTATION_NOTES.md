# LeetCode-Style Problem Solving Interface - Implementation Guide

## Overview
The problem-solving interface has been completely redesigned with a LeetCode-like layout featuring resizable panels using an adjustable vertical divider and horizontal divider for the problem section.

## Layout Structure

### Main Panel Arrangement (Vertical Split)
- **Left Panel (50% default)**: Code Editor + Submit Button
- **Vertical Divider**: Adjustable divider to resize left/right panels (drag to adjust)
- **Right Panel (50% default)**: Split into two sections horizontally

### Right Panel (Horizontal Split)
- **Top Section (50% default)**: Problem Description + Sample Input/Output
- **Horizontal Divider**: Adjustable divider to resize top/bottom sections (drag to adjust)
- **Bottom Section (50% default)**: Custom Test Cases + Test Results

## Features

### 1. Resizable Panels
- **Vertical Divider**: Drag left/right to resize code editor vs. problem sections
- **Horizontal Divider**: Drag up/down to resize problem description vs. test cases
- **Constraints**: Panels cannot be resized below 20% or above 80% of available space
- **Visual Feedback**: Dividers highlight with accent color on hover and while dragging

### 2. Code Editor (Left Panel)
- **Monaco Editor**: Full-featured code editor with syntax highlighting
- **Java Support**: Write your Java solution code
- **Line Numbers & Minimap**: Disabled minimap for cleaner interface
- **Font Size**: 14px for comfortable reading
- **Submit Button**: Below the editor to submit your solution

### 3. Problem Description Panel (Right Top)
- **Problem Title**: Large heading with problem number
- **Difficulty & Tags**: Quick reference for problem difficulty and topics
- **Description**: Full problem description with markdown formatting
- **Input/Output Format**: Detailed specification of input and output requirements
- **Constraints**: Problem constraints and limitations
- **Sample Input/Output**: Example test case with expected input/output

### 4. Custom Test Cases Panel (Right Bottom)
- **Input Textarea**: Enter your custom test input
- **Run Button**: Execute your code with the custom input
- **Test Results**: Display results in card format (latest first)
- **Output Display**: Shows program output for successful runs
- **Error Display**: Shows compilation, runtime, or execution errors

## How to Use

### Submitting Your Solution
1. Write your Java code in the code editor (left panel)
2. Click the "Submit Java" button to submit against sample test cases
3. A modal will show the verdict (ACCEPTED, WRONG_ANSWER, COMPILATION_ERROR, etc.)

### Running Custom Tests
1. Enter your custom test input in the text area (right bottom panel)
2. Click the "Run" button to execute your code
3. The results will appear above with your input and the corresponding output
4. Multiple test runs are stored and displayed in reverse chronological order

### Adjusting Panels
1. **Vertical Resize**: Click and drag the vertical divider between code and problem panels
2. **Horizontal Resize**: Click and drag the horizontal divider between problem and test case panels
3. Release to set new panel sizes

## Backend Endpoints

### 1. Submit Solution
**Endpoint**: `POST /api/submissions`
- **Purpose**: Submit code for evaluation against sample test cases
- **Request**:
  ```json
  {
    "problemId": 1,
    "language": "JAVA",
    "code": "..."
  }
  ```
- **Response**: Includes verdict, expected output, actual output, and execution message

### 2. Execute Custom Test
**Endpoint**: `POST /api/executions/test`
- **Purpose**: Run code with custom input for testing
- **Request**:
  ```json
  {
    "problemId": 1,
    "language": "JAVA",
    "code": "...",
    "input": "your test input here"
  }
  ```
- **Response**: Includes output, error (if any), and execution time in milliseconds

## New Components Created

### Frontend Components
1. **ResizablePanelLayout.tsx**: Main layout component with split panels and draggable dividers
2. **ProblemDescriptionPanel.tsx**: Displays problem details fetched from API
3. **CustomTestCases.tsx**: Handles custom test execution and result display
4. **Updated CodeEditor.tsx**: Made responsive to fit the new layout
5. **Updated solve/page.tsx**: Integrated all components into the new layout

### Backend Components
1. **ExecutionRequest.java**: DTO for custom test execution requests
2. **ExecutionResponse.java**: DTO for execution response data
3. **ExecutionService.java**: Service handling code compilation and execution
4. **ExecutionController.java**: REST controller exposing the execution endpoint

## Responsive Design

The interface adapts to different screen sizes:
- **Full-screen editor**: Takes the entire viewport (except navbar and sidebar)
- **Fixed positioning**: Solves prevent layout shifts while resizing
- **Sidebar offset**: On medium screens and up, respects 240px space for sidebar
- **Overflow handling**: Problem description and test results scroll independently

## Color Scheme

The interface uses the existing platform color scheme:
- **Background**: `#000000`
- **Panel**: `#0a0a0a`
- **Border**: `#16320f`
- **Accent**: `#22c55e` (green)
- **Text**: `#f3f4f6`

## Security

- All endpoints require authentication except for problem details
- Code execution runs in isolated temporary directories
- Temp files are cleaned up after execution
- Execution includes timeouts (compilation: 10s, execution: 5s)
- Time limits prevent infinite loops and memory exhaustion

## Next Steps / Future Improvements

1. Add language selector (currently Java only)
2. Implement syntax highlighting configuration per language
3. Add code templates for different languages
4. Store custom test cases for future reference
5. Add performance metrics (execution time, memory usage)
6. Implement collaborative problem solving
7. Add test case sharing feature
8. Create test case difficulty levels

## Technical Details

- **Frontend Framework**: Next.js 13+ with React
- **Language**: TypeScript
- **Styling**: Tailwind CSS
- **Code Editor**: Monaco Editor
- **Backend**: Spring Boot (Java 21)
- **Build Tool**: Maven
- **Process Execution**: Java ProcessBuilder for safe code execution
