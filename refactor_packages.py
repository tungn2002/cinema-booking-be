import os
import glob

replacements = {
    "com.personal.cinemabooking.controller": "com.personal.cinemabooking.controllers",
    "com.personal.cinemabooking.config": "com.personal.cinemabooking.configs",
    "com.personal.cinemabooking.entity": "com.personal.cinemabooking.entities",
    "com.personal.cinemabooking.repo": "com.personal.cinemabooking.repositories",
    "com.personal.cinemabooking.service": "com.personal.cinemabooking.services",
    "com.personal.cinemabooking.util": "com.personal.cinemabooking.utils",
    "com.personal.cinemabooking.exception": "com.personal.cinemabooking.core.exceptions"
}

# Recursively find all .java files
java_files = glob.glob('src/main/java/**/*.java', recursive=True)

for filepath in java_files:
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    modified = False
    for old, new in replacements.items():
        if old in content:
            content = content.replace(old, new)
            modified = True
            
    if modified:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(content)
        print(f"Updated {filepath}")
