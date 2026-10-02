# Complaint Management Portal (Java Servlets + JDBC + MySQL)

Beginner college project. Users register, log in and submit complaints.
Admin sees all users, all complaints, an activity log, and can change complaint status.

**Default admin login:** `admin@college.com` / `admin123`

## Tech stack
| Layer | Tech |
|---|---|
| Frontend | HTML, CSS, JavaScript (fetch API) |
| Backend | Java 17 Servlets (Jakarta Servlet 6) on Tomcat 10.1 |
| Database access | JDBC with PreparedStatement |
| Database | MySQL 8 |
| Build | Maven (WAR file) |

## Flow
Browser page -> fetch() -> Servlet -> JDBC -> MySQL -> JSON back -> page updates

## Folder structure
```
database/schema.sql              tables + default admin
src/main/java/com/college/
   util/      DBConnection, PasswordUtil, ActivityLogger, JsonUtil, SessionUtil
   servlet/   Register, Login, Logout, Me, Complaint, Admin servlets
src/main/webapp/
   login.html  user.html  admin.html
   css/style.css   js/app.js
   WEB-INF/web.xml
pom.xml
```

## Setup (local)

### 1. Install
- JDK 17 (adoptium.net). Check: `java -version`
- Maven (maven.apache.org). Check: `mvn -v`
- MySQL Server 8 + Workbench (dev.mysql.com)
- Tomcat 10.1 (tomcat.apache.org, download the zip). Must be 10.1, NOT 9.

### 2. Create the database
Option A (command line, from the project folder):
    mysql -u root -p < database/schema.sql
Option B: open database/schema.sql in MySQL Workbench and click the lightning button.

### 3. Set your MySQL password
Easiest (nothing to edit): set an environment variable in the same window before starting Tomcat.
    PowerShell:  $env:DB_PASSWORD = "your_real_password"
    CMD:         set DB_PASSWORD=your_real_password
Or edit the default in `src/main/java/com/college/util/DBConnection.java`.
Never commit your real password to GitHub.

### 4. Build
From the project folder:
    mvn clean package
Result: `target/complaintportal.war`

### 5. Deploy on Tomcat
1. Copy `target/complaintportal.war` into Tomcat's `webapps/` folder.
2. Start Tomcat:
   - Windows: `bin\startup.bat`  (needs JAVA_HOME set to your JDK folder)
   - Mac/Linux: `bin/startup.sh`  (run `chmod +x bin/*.sh` first if needed)
3. Open http://localhost:8080/complaintportal/

To stop: `bin\shutdown.bat` / `bin/shutdown.sh`
To see errors live: run `bin\catalina.bat run` instead of startup.bat.

After changing any Java file: `mvn clean package`, stop Tomcat, delete
`webapps/complaintportal` (the folder) and the old .war, copy the new .war, start again.

## Quick test checklist
1. Register a new user, then log in -> goes to user.html
2. Submit 2 complaints -> they appear in "My complaints" with PENDING
3. Log out, log in as admin@college.com / admin123 -> goes to admin.html
4. Admin sees user count, complaints, activity log
5. Change a complaint to RESOLVED -> log in as that user, status updated
6. While logged in as a normal user, open http://localhost:8080/complaintportal/api/admin
   -> you should get 403 "Admins only."

## Troubleshooting
| Problem | Fix |
|---|---|
| `Access denied for user 'root'` | Wrong password in DBConnection.java |
| `Unknown database 'complaint_db'` | schema.sql was not run |
| 404 on the page | WAR name must be complaintportal.war; URL must include /complaintportal/ |
| `ClassNotFoundException` for MySQL driver | Rebuild with `mvn clean package`, make sure pom.xml is unchanged |
| `ClassNotFoundException: javax.servlet...` or servlets not working | You are on Tomcat 9. Use Tomcat 10.1 |
| Port 8080 busy | Change the port in Tomcat's conf/server.xml |
| Changes not showing | Redeploy as described above and hard-refresh the browser (Ctrl+F5) |

## Security features (good for viva)
- PreparedStatement everywhere -> prevents SQL injection
- Passwords stored as SHA-256 hashes, never plain text
- Role is checked on the SERVER (AdminServlet returns 401/403), not only in the page
- Pages use textContent, not innerHTML -> prevents XSS from complaint text
- Session is replaced on login; logout invalidates it
- Admin API never sends the password column

## Limitations / future scope (use in report Chapter 6)
- SHA-256 without salt is weak; real systems use bcrypt or Argon2 with salt
- No CSRF tokens; no HTTPS; no login attempt limit
- No password reset, no pagination, no complaint edit/delete
- DB password is hard-coded; should come from environment/config
- Admin account is created by SQL; no admin management screen

## How to explain it in a viva
**Login flow:** Page sends email+password to /api/login. LoginServlet hashes the password,
runs `SELECT ... WHERE email=? AND password=?`. If a row matches, it stores userId, name and role
in the HttpSession and the page redirects by role.

**Admin protection:** Every request to /api/admin goes through checkAdmin(): no session -> 401,
session but role is not ADMIN -> 403. Hiding a page is not security; the server check is.

**Activity log:** ActivityLogger.log() is called after register, login, logout, complaint submit
and status change. The admin page reads the latest 100 rows.

## Deploying online on Render only (web service + PostgreSQL)
1. Push the project to GitHub (no real passwords in the code).
2. Render -> New -> PostgreSQL (Free). Copy its **Internal Database URL**.
3. Render -> New -> Web Service -> your repo -> Language: Docker -> Free.
4. Environment variables:
   - DATABASE_URL = the Internal Database URL  (use External URL if Internal fails)
   - PORT = 8080
   - ADMIN_PASSWORD = a strong password for the first admin (optional)
5. Deploy. Tables and the admin account are created automatically at startup
   (DatabaseInitializer.java). No manual SQL needed.

The same code still runs on local MySQL when DATABASE_URL is not set.
Alternative: keep MySQL on Aiven and set DB_URL / DB_USER / DB_PASSWORD instead.
"# Complaint-portal" 
