# Build Fix Instructions

## Current Issue

**Problem:** Maven compilation fails due to Java version mismatch
- System Java version: **Java 23**
- Project configuration: **Java 17**

**Error:**
```
Fatal error compiling: java.lang.ExceptionInInitializerError: 
com.sun.tools.javac.code.TypeTag :: UNKNOWN
```

---

## ✅ Current Workaround (Active Now)

Running the pre-built JAR file:
```bash
java -jar target/fit4ever-0.0.1-SNAPSHOT.jar
```

This works for accessing Swagger UI and taking screenshots!

---

## 🔧 Permanent Solutions

### Solution 1: Install Java 17 (Recommended)

#### Using Homebrew:
```bash
# Install OpenJDK 17
brew install openjdk@17

# Create symlink
sudo ln -sfn /opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk \
  /Library/Java/JavaVirtualMachines/openjdk-17.jdk

# Add to ~/.zshrc
echo 'export PATH="/opt/homebrew/opt/openjdk@17/bin:$PATH"' >> ~/.zshrc
echo 'export JAVA_HOME="/opt/homebrew/opt/openjdk@17"' >> ~/.zshrc

# Reload
source ~/.zshrc

# Verify
java -version
mvn -version
```

---

### Solution 2: Use SDKMAN (Easy Version Management)

```bash
# Install SDKMAN
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"

# List available Java versions
sdk list java

# Install Java 17 (Temurin)
sdk install java 17.0.9-tem

# Set as default
sdk default java 17.0.9-tem

# Or use for current session only
sdk use java 17.0.9-tem

# Verify
java -version
```

**Benefits:**
- Easy switching between Java versions
- Per-project Java version with `.sdkmanrc`
- Clean uninstall/reinstall

---

### Solution 3: Update Project to Java 23

If you want to use Java 23, update `pom.xml`:

```xml
<properties>
    <java.version>23</java.version>
    <lombok.version>1.18.32</lombok.version>
</properties>
```

And update the compiler plugin:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <version>3.13.0</version>  <!-- Updated version -->
    <configuration>
        <source>23</source>
        <target>23</target>
        <release>23</release>
        <annotationProcessorPaths>
            <path>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
                <version>${lombok.version}</version>
            </path>
        </annotationProcessorPaths>
    </configuration>
</plugin>
```

**Note:** Java 23 is very new. Stick with Java 17 (LTS) for production projects.

---

## 🧹 Clean Build After Fix

Once Java 17 is installed:

```bash
# Clean previous build
mvn clean

# Rebuild project
mvn clean package

# Run application
mvn spring-boot:run

# Or run the JAR
java -jar target/fit4ever-0.0.1-SNAPSHOT.jar
```

---

## 🎯 For Your LinkedIn Post (Right Now)

**You don't need to fix this immediately!**

The application is running using the pre-built JAR. You can:

1. ✅ Access Swagger UI: http://localhost:8080/swagger-ui/index.html
2. ✅ Take screenshots
3. ✅ Create architecture diagrams
4. ✅ Post to LinkedIn

Fix the Java version later when you have time.

---

## 🆘 Troubleshooting

### Check Current Java Version
```bash
java -version
mvn -version
which java
```

### Multiple Java Installations
```bash
# List all Java installations (Mac)
/usr/libexec/java_home -V

# Use specific version
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
```

### Maven Not Using Correct Java
```bash
# Set in Maven settings
export JAVA_HOME=/path/to/java17
mvn -version  # Should show Java 17
```

### Clean Maven Cache
```bash
# If builds are still failing
rm -rf ~/.m2/repository
mvn clean install
```

---

## 📋 Quick Reference

| Command | Purpose |
|---------|---------|
| `java -version` | Check Java version |
| `mvn -version` | Check Maven and Java version |
| `mvn clean` | Clean build artifacts |
| `mvn clean package` | Build JAR file |
| `mvn spring-boot:run` | Run application |
| `java -jar target/*.jar` | Run pre-built JAR |

---

## 🎓 Why Java 17?

- **LTS (Long Term Support)**: Supported until 2029
- **Spring Boot 3.x**: Requires Java 17 minimum
- **Industry Standard**: Most companies use Java 11 or 17
- **Stability**: Well-tested and production-ready

---

## ✅ Recommended: Install Java 17

For professional development, install Java 17 alongside Java 23:

```bash
# Install Java 17 with Homebrew
brew install openjdk@17

# Use SDKMAN to manage versions
sdk install java 17.0.9-tem
sdk default java 17.0.9-tem

# Verify
java -version  # Should show "17.x.x"
```

Then rebuild:
```bash
mvn clean package
mvn spring-boot:run
```

---

**For now:** Your app is running! Go take those screenshots! 📸



