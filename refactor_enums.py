import os
import glob
import re

java_files = glob.glob('src/main/java/com/personal/cinemabooking/**/*.java', recursive=True)

payment_enum_code = r'''    // payment statuses - matches stripe statuses
    public enum PaymentStatus {
        PENDING,  // initial state
        SUCCEEDED, // payment completed successfully
        FAILED,   // payment failed
        REFUNDED  // payment was refunded
    }'''

review_enum_code = r'''    // possible review statuses
    public enum ReviewStatus {
        PENDING,   // waiting for approval
        APPROVED,  // visible to users
        REJECTED   // not shown (spam/inappropriate)
    }'''

for filepath in java_files:
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    modified = False

    if filepath.endswith('Payment.java'):
        if payment_enum_code in content:
            content = content.replace(payment_enum_code, '')
            modified = True
        if 'import com.personal.cinemabooking.enums.PaymentStatus;' not in content:
            content = content.replace('package com.personal.cinemabooking.entities;', 'package com.personal.cinemabooking.entities;\n\nimport com.personal.cinemabooking.enums.PaymentStatus;')
            modified = True

    elif filepath.endswith('Review.java'):
        if review_enum_code in content:
            content = content.replace(review_enum_code, '')
            modified = True
        if 'import com.personal.cinemabooking.enums.ReviewStatus;' not in content:
            content = content.replace('package com.personal.cinemabooking.entities;', 'package com.personal.cinemabooking.entities;\n\nimport com.personal.cinemabooking.enums.ReviewStatus;')
            modified = True
    else:
        # replace Payment.PaymentStatus with PaymentStatus
        if 'Payment.PaymentStatus' in content:
            content = content.replace('Payment.PaymentStatus', 'PaymentStatus')
            if 'import com.personal.cinemabooking.enums.PaymentStatus;' not in content:
                content = re.sub(r'(package .*;\n)', r'\1\nimport com.personal.cinemabooking.enums.PaymentStatus;\n', content, 1)
            modified = True
        
        # replace Review.ReviewStatus with ReviewStatus
        if 'Review.ReviewStatus' in content:
            content = content.replace('Review.ReviewStatus', 'ReviewStatus')
            if 'import com.personal.cinemabooking.enums.ReviewStatus;' not in content:
                content = re.sub(r'(package .*;\n)', r'\1\nimport com.personal.cinemabooking.enums.ReviewStatus;\n', content, 1)
            modified = True

    if modified:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(content)
        print(f"Updated {filepath}")
