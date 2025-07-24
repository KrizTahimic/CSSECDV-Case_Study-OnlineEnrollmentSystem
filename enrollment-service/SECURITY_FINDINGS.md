# Enrollment Service Security Findings

## Critical Security Vulnerability Discovered

### Summary
The Enrollment Service has a **CRITICAL** security vulnerability where all enrollment endpoints are publicly accessible without any authentication or authorization requirements.

### Details

#### Location
- **File**: `src/main/java/com/enrollment/enrollment/config/SecurityConfig.java`
- **Line**: 37
- **Code**: `.requestMatchers("/api/enrollments/**").permitAll()`

#### Impact
This vulnerability allows:
1. **Unauthorized Access**: Anyone can view, create, or delete enrollments without authentication
2. **Data Breach**: Student enrollment data can be accessed by anyone
3. **Service Manipulation**: Malicious users can:
   - Enroll/unenroll any student from any course
   - View all student enrollments
   - View all course enrollments
   - Manipulate enrollment counts

#### Affected Endpoints
All enrollment endpoints are exposed:
- `GET /api/enrollments` - View enrollments
- `GET /api/enrollments/student/{studentId}` - View specific student's enrollments
- `GET /api/enrollments/course/{courseId}` - View course enrollments
- `POST /api/enrollments` - Create enrollments
- `POST /api/enrollments/student/{studentId}/course/{courseId}` - Enroll student
- `DELETE /api/enrollments/student/{studentId}/course/{courseId}` - Unenroll student

### Security Requirements Violated
According to CSSECDV requirements in checklist.md:
- **2.1.1**: "Require authentication for all pages and resources, except those specifically intended to be public"
- **2.2.1**: "Use a single site-wide component to check access authorization"
- **2.2.2**: "Access controls should fail securely"

### Recommended Fix
1. Change security configuration to require authentication:
   ```java
   .requestMatchers("/api/enrollments/**").authenticated()
   ```

2. Implement role-based access control:
   - Students: Can only view/manage their own enrollments
   - Faculty: Can view enrollments for their courses
   - Admin: Can view/manage all enrollments

3. Add @PreAuthorize annotations to controller methods for fine-grained access control

### Risk Level
**CRITICAL** - This vulnerability exposes sensitive student data and allows unauthorized manipulation of the enrollment system.

### Discovery Date
2025-07-24

### Status
✅ **PATCHED** - Security fixes have been implemented

## UPDATE: Security Vulnerabilities Fixed ✅

The critical security vulnerabilities documented above have been addressed:

### Fixes Implemented:
1. ✅ **Authentication Required**: Changed `.permitAll()` to `.authenticated()` for all enrollment endpoints
2. ✅ **Role-Based Authorization**: Added @PreAuthorize annotations to all controller methods:
   - Students can only view/manage their own enrollments
   - Faculty can view enrollments for courses
   - Admin has full access to all enrollment operations
3. ✅ **Method Security Enabled**: Added @EnableMethodSecurity to SecurityConfig

### Authorization Matrix:
| Endpoint | Student | Faculty | Admin |
|----------|---------|---------|-------|
| GET /api/enrollments | ✅ Own only | ✅ | ✅ |
| GET /api/enrollments/student/{id} | ✅ Own only | ✅ | ✅ |
| GET /api/enrollments/course/{id} | ❌ | ✅ | ✅ |
| POST /api/enrollments | ✅ Self only | ❌ | ❌ |
| POST /api/enrollments/student/{id}/course/{id} | ❌ | ❌ | ✅ |
| DELETE /api/enrollments/student/{id}/course/{id} | ✅ Own only | ❌ | ✅ |

### Fix Date
2025-07-24