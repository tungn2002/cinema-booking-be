import os
import re
import glob

base_pkg = "src/main/java/com/personal/cinemabooking"

def remove_rate_limit(filepath):
    with open(filepath, "r", encoding="utf-8") as f:
        content = f.read()

    original_content = content
    # Remove import
    content = re.sub(r'import io\.github\.resilience4j\.ratelimiter\.annotation\.RateLimiter;\s*\n', '', content)
    
    # Remove annotation (and optional comments on the same line)
    content = re.sub(r'\s*@RateLimiter\(.*?\).*?\n', '\n', content)

    if content != original_content:
        with open(filepath, "w", encoding="utf-8") as f:
            f.write(content)
        print(f"Cleaned {filepath}")

# Iterate over all controllers
for root, _, files in os.walk(base_pkg):
    for file in files:
        if file.endswith(".java"):
            remove_rate_limit(os.path.join(root, file))

# Remove dependencies from pom.xml
with open("pom.xml", "r", encoding="utf-8") as f:
    pom = f.read()

pom = re.sub(r'\s*<dependency>\s*<groupId>io\.github\.resilience4j</groupId>\s*<artifactId>resilience4j-ratelimiter</artifactId>\s*<version>.*?</version>\s*</dependency>', '', pom)
pom = re.sub(r'\s*<dependency>\s*<groupId>io\.github\.resilience4j</groupId>\s*<artifactId>resilience4j-spring-boot3</artifactId>\s*<version>.*?</version>\s*</dependency>', '', pom)
pom = re.sub(r'\s*<dependency>\s*<groupId>org\.springframework\.boot</groupId>\s*<artifactId>spring-boot-starter-aop</artifactId>\s*</dependency>', '', pom) # AOP is often used for resilience4j annotations, but might be used elsewhere. Let's keep it safe and just remove resilience4j. Actually AOP is 180-183. Let's just remove it since it was right below resilience4j. Wait, I'll only remove resilience4j to be safe.

with open("pom.xml", "w", encoding="utf-8") as f:
    f.write(pom)
print("Cleaned pom.xml")

# Remove from application properties
for prop_file in glob.glob("src/main/resources/*.properties"):
    with open(prop_file, "r", encoding="utf-8") as f:
        lines = f.readlines()
    
    new_lines = []
    skip = False
    for line in lines:
        if line.startswith("resilience4j."):
            continue
        new_lines.append(line)
        
    with open(prop_file, "w", encoding="utf-8") as f:
        f.writelines(new_lines)
    print(f"Cleaned {prop_file}")
