import xml.etree.ElementTree as ET

tree = ET.parse("data/dump.xml")
for n in tree.iter():
    txt = n.attrib.get("text")
    if txt:
        print(f"'{txt}': {n.attrib.get('bounds')}")
