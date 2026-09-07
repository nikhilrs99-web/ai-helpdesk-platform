import sys
import re

msg = sys.stdin.read()
# Replace "Complete Phase X (Days...)" with "Complete Phase X"
new_msg = re.sub(r'Complete Phase (\d+).*', r'Complete Phase \1', msg)
sys.stdout.write(new_msg)
