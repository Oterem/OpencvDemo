from skimage import segmentation as seg

from skimage import io, exposure, img_as_uint, img_as_float
import PIL.ImageGrab
from PIL import Image
from xlwt import Workbook
import xlrd
import numpy as np
import cv2
from os import listdir

x_cor = 0
y_cor = 0
flag = 0


def mean_color(image, labels):
    out = np.zeros_like(image)
    for label in np.unique(labels):
        indices = np.nonzero(labels == label)
        out[indices] = np.mean(image[indices], axis=0)
    return out


def plot_slic(img, ns, c, s):
    labels = seg.slic(img, n_segments=ns, compactness=c, sigma=sigma)
    return mean_color(img, labels)


ns = 12
compact = 70
sigma = 5.0
source = 'C:\challenge\Superpixel\scaled'
super_pixel_dest = 'C:\challenge\Superpixel\super_pixeled'
segmented_path = 'C:\challenge\Superpixel\segmented'
wb = xlrd.open_workbook('C:\challenge\Superpixel\mole_coordinates.xls')
worksheet = wb.sheet_by_name('Sheet 1')
names = listdir(source)
i = 1
counter = 1
length = len(names)
for img_name in names:
    a = cv2.imread(source + '\\' + img_name, 1)
    img = Image.open(source + '\\' + img_name)
    width, height = img.size
    img1 = img_as_float(a)
    x = worksheet.cell(i, 1).value
    y = worksheet.cell(i, 2).value
    calc = plot_slic(img1, ns, compact, sigma)
    res = calc / (calc.max() / 255.0)
    path = super_pixel_dest + '\pixeled_' + img_name
    cv2.imwrite(path, res)
    image = Image.open(path)
    a = np.asarray(image)
    a = cv2.cvtColor(a, cv2.COLOR_RGB2BGR)
    blur = cv2.bilateralFilter(a, 9, 75, 75)
    pix = image.load()
    lower = np.array(pix[x, y])
    upper = np.array(pix[x, y])
    mask = np.zeros
    I = np.array(blur)
    I = cv2.cvtColor(I, cv2.COLOR_BGR2RGB)
    mask = cv2.inRange(I, lower, upper)
    cv2.imwrite(segmented_path + '\\' + img_name, mask)
    i += 1
    print(length - counter, ' left')
    counter += 1
