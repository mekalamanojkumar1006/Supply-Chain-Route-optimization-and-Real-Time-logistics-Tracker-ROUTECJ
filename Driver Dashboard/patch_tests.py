import os
import re

test_dir = r'H:\adkprojectfile\RouteCJDriver\app\src\test\java\com\routecj\driver\domain\usecase'

def inject_method(content, interface_name, method_code):
    # Find all classes that implement the interface
    pattern = r'class\s+\w+\s*(?:\([^)]*\))?\s*:\s*' + interface_name + r'\s*{'
    matches = list(re.finditer(pattern, content))
    
    offset = 0
    for match in matches:
        start_idx = match.end()
        # Find the matching closing brace
        brace_count = 1
        i = start_idx
        while i < len(content) and brace_count > 0:
            if content[i] == '{':
                brace_count += 1
            elif content[i] == '}':
                brace_count -= 1
            i += 1
        
        if brace_count == 0:
            # We found the closing brace at i-1
            insert_pos = i - 1
            # Check if it already has it
            class_body = content[start_idx:insert_pos]
            method_name = method_code.split('fun ')[1].split('(')[0]
            if method_name not in class_body:
                content = content[:insert_pos] + '    ' + method_code + '\n    ' + content[insert_pos:]
                offset += len('    ' + method_code + '\n    ')
    return content

for root, _, files in os.walk(test_dir):
    for f in files:
        if f.endswith('.kt'):
            path = os.path.join(root, f)
            with open(path, 'r', encoding='utf-8') as file:
                content = file.read()
            
            # Clean up bad powershell inserts
            content = re.sub(r'^\s*override suspend fun getDispatchHistory\(.*?\): List<Dispatch> = emptyList\(\)\s*\n?', '', content, flags=re.MULTILINE)
            content = re.sub(r'^\s*override suspend fun getOrderHistory\(.*?\): List<Order> = emptyList\(\)\s*\n?', '', content, flags=re.MULTILINE)
            
            content = inject_method(content, 'DispatchRepository', 'override suspend fun completeTrip(dispatchId: String, driverId: String): com.routecj.driver.core.util.Result<Unit> = com.routecj.driver.core.util.Result.Success(Unit)')
            content = inject_method(content, 'OrderRepository', 'override suspend fun completeOrderTrip(orderId: String, driverId: String): com.routecj.driver.core.util.Result<Unit> = com.routecj.driver.core.util.Result.Success(Unit)')
            
            with open(path, 'w', encoding='utf-8') as file:
                file.write(content)
print('Patching complete')
