import sys, json, collections
from pathlib import Path

analysis = json.loads(Path('graphify-out/.graphify_analysis.json').read_text(encoding='utf-8'))
labels = {}
for k, v in analysis['communities'].items():
    if not v: continue
    # just pick the most common word in the labels
    words = []
    for node in v:
        # node format could be something like label or just a string
        if isinstance(node, dict):
            words.extend(node.get('id', '').split('_'))
        elif isinstance(node, str):
            words.extend(node.split('_'))
    
    words = [w for w in words if len(w) > 3]
    if words:
        common = collections.Counter(words).most_common(2)
        labels[int(k)] = " ".join([c[0].capitalize() for c in common])
    else:
        labels[int(k)] = f"Community {k}"

Path('graphify-out/.graphify_labels_gen.json').write_text(json.dumps(labels, indent=2), encoding='utf-8')
print("Generated labels")
