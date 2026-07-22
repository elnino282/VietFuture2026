import os
import re

def process_file(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    if '@Entity' not in content or '@Data' not in content:
        return False

    # Find fields with relationship annotations
    # Pattern looks for @OneToMany, @ManyToOne, @ManyToMany, @OneToOne
    # and extracts the field name.
    
    # This regex is more robust: it finds the annotation, and then skips to the semicolon or equals sign,
    # capturing the last word before the assignment or semicolon as the field name.
    relation_pattern = re.compile(
        r'@(?:OneToMany|ManyToOne|OneToOne|ManyToMany)[\s\S]*?(?:\w+[\s<>,\?\[\]]+)+\s+(\w+)\s*(?:=|;)'
    )
    
    # Wait, the above pattern could match too much if there are multiple annotations before a field.
    # A safer approach is to split the content by ';' and look for relation annotations in each statement.
    statements = content.split(';')
    fields = []
    for stmt in statements:
        if any(ann in stmt for ann in ['@OneToMany', '@ManyToOne', '@OneToOne', '@ManyToMany']):
            # Clean up comments inside the statement to avoid false matches
            stmt_no_comments = re.sub(r'//.*', '', stmt)
            stmt_no_comments = re.sub(r'/\*.*?\*/', '', stmt_no_comments, flags=re.DOTALL)
            
            # The field name is typically the last word before the equals sign or at the end of the statement
            # Remove any assignments e.g., = new ArrayList<>()
            stmt_no_assignment = stmt_no_comments.split('=')[0].strip()
            
            # The last word is the field name
            parts = stmt_no_assignment.split()
            if parts:
                field_name = parts[-1]
                # Filter out valid Java identifiers
                if re.match(r'^[a-zA-Z_$][a-zA-Z\d_$]*$', field_name):
                    fields.append(field_name)

    if not fields:
        return False

    # Format the exclude string
    if len(fields) == 1:
        exclude_str = f'"{fields[0]}"'
    else:
        exclude_str = "{" + ", ".join(f'"{f}"' for f in fields) + "}"
        
    replacement = f'@Getter\n@Setter\n@ToString(exclude = {exclude_str})\n@EqualsAndHashCode(exclude = {exclude_str})'
    
    new_content = content.replace('@Data', replacement)
    new_content = re.sub(r'import lombok\.Data;', 'import lombok.*;', new_content)
    
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(new_content)
        
    print(f"Updated {filepath} with excluded fields: {fields}")
    return True

def main():
    updated_count = 0
    for root, _, files in os.walk('.'):
        for file in files:
            if file.endswith('.java'):
                filepath = os.path.join(root, file)
                try:
                    if process_file(filepath):
                        updated_count += 1
                except Exception as e:
                    print(f"Error processing {filepath}: {e}")
                    
    print(f"Total files updated: {updated_count}")

if __name__ == "__main__":
    main()
