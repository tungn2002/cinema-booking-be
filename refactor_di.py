import os
import glob
import re

controllers = glob.glob('src/main/java/com/personal/cinemabooking/controllers/*.java')

for filepath in controllers:
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    modified = False
    
    # 1. Add @RequiredArgsConstructor if not present
    if '@RequiredArgsConstructor' not in content and '@RestController' in content:
        content = content.replace('@RestController', '@RestController\n@RequiredArgsConstructor')
        modified = True
        
    # Add lombok import if needed
    if 'import lombok.RequiredArgsConstructor;' not in content and '@RequiredArgsConstructor' in content:
        content = re.sub(r'(package .*;\n)', r'\1\nimport lombok.RequiredArgsConstructor;\n', content, 1)
        modified = True

    # 2. Replace @Autowired fields with private final
    # Regex looks for @Autowired followed by some whitespace/newlines and then private Type name;
    autowired_pattern = r'@Autowired\s*(?://.*?\n)?\s*private\s+([A-Za-z0-9_<>]+)\s+([A-Za-z0-9_]+)\s*;'
    
    if re.search(autowired_pattern, content):
        content = re.sub(autowired_pattern, r'private final \1 \2;', content)
        modified = True
        
    # 3. Clean up any leftover @Autowired on constructors if they exist
    # (Optional, but good practice)
    
    if modified:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(content)
        print(f"Refactored DI for {os.path.basename(filepath)}")
