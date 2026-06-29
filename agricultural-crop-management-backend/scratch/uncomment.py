import os

controller_dir = r"c:\Users\ACER\Documents\GitHub\SE122-Code-MicroserviceReady-Test\agricultural-crop-management-backend\src\main\java\org\example\QuanLyMuaVu\module\marketplace\controller"

for filename in os.listdir(controller_dir):
    if filename.endswith(".java"):
        filepath = os.path.join(controller_dir, filename)
        with open(filepath, "r", encoding="utf-8") as f:
            content = f.read()
        
        # Uncomment @RestController and @RequestMapping
        new_content = content
        if "// @RestController" in new_content:
            new_content = new_content.replace("// @RestController", "@RestController")
        if "// @RequestMapping" in new_content:
            # We need to find the specific commented @RequestMapping line and uncomment it
            # e.g., // @RequestMapping("/api/v1/marketplace...") -> @RequestMapping("/api/v1/marketplace...")
            lines = new_content.splitlines()
            for i, line in enumerate(lines):
                if line.strip().startswith("// @RequestMapping"):
                    lines[i] = line.replace("// @RequestMapping", "@RequestMapping")
            new_content = "\n".join(lines)
            
        if new_content != content:
            print(f"Uncommenting in {filename}")
            with open(filepath, "w", encoding="utf-8") as f:
                f.write(new_content)
