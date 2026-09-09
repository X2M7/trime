# SPDX-License-Identifier: GPL-3.0-or-later
import unittest
import xml.etree.ElementTree as ET

from PIL import Image, ImageDraw

from editor_pixels import audit_t9_pixels


class EditorPixelsTest(unittest.TestCase):
    def fixture(self):
        root = ET.Element('hierarchy')
        image = Image.new('RGB', (400, 200), 'white')
        draw = ImageDraw.Draw(image)
        for i, label in enumerate(('2 ABC', '3 DEF', '4 GHI', '5 JKL', '6 MNO', '7 PQRS', '8 TUV', '9 WXYZ')):
            x, y = i % 4 * 100, i // 4 * 100
            ET.SubElement(root, 'node', {'content-desc': label, 'bounds': f'[{x},{y}][{x + 100},{y + 100}]'})
            draw.text((x + 30, y + 40), label, fill='black')
        return image, root

    def test_visible_keys_pass(self):
        image, root = self.fixture()
        self.assertTrue(audit_t9_pixels(image, root, image.size)['passed'])

    def test_blank_surface_fails_despite_accessibility(self):
        image, root = self.fixture()
        image.paste('white', (0, 0, 400, 200))
        self.assertFalse(audit_t9_pixels(image, root, image.size)['passed'])

    def test_one_blank_key_fails(self):
        image, root = self.fixture()
        image.paste('white', (0, 0, 100, 100))
        self.assertFalse(audit_t9_pixels(image, root, image.size)['passed'])

    def test_borders_and_one_pixel_are_not_text(self):
        image, root = self.fixture()
        draw = ImageDraw.Draw(image)
        draw.rectangle((0, 0, 99, 99), fill='white', outline='black')
        draw.point((50, 50), fill='black')
        self.assertFalse(audit_t9_pixels(image, root, image.size)['passed'])

    def test_rotation_and_out_of_bounds_fail(self):
        image, root = self.fixture()
        with self.assertRaises(ValueError):
            audit_t9_pixels(image, root, (200, 400))
        root[0].set('bounds', '[0,0][401,100]')
        with self.assertRaises(ValueError):
            audit_t9_pixels(image, root, image.size)

    def test_missing_and_duplicate_keys_fail(self):
        image, root = self.fixture()
        root.remove(root[0])
        with self.assertRaises(ValueError):
            audit_t9_pixels(image, root, image.size)
        image, root = self.fixture()
        ET.SubElement(root, 'node', dict(root[0].attrib))
        with self.assertRaises(ValueError):
            audit_t9_pixels(image, root, image.size)


if __name__ == '__main__':
    unittest.main()
