from skimage import segmentation as seg
from skimage import io, exposure, img_as_uint, img_as_float
import PIL.ImageGrab
from PIL import Image
from xlwt import Workbook
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
    labels = seg.slic(img, n_segments=ns, compactness=c)

    return mean_color(img, labels)


def click_event(event, x, y, flags, param):
    if event == cv2.EVENT_LBUTTONDOWN:

        x_cor = x
        y_cor = y
        print(x_cor,y_cor)
        sheet1.write(param, 1, x_cor)
        sheet1.write(param, 2, y_cor)

        calc = plot_slic(img1, ns, compact, sigma)
        res = calc / (calc.max() / 255.0)
        path = super_pixel_dest+'\pixeled_'+img_name
        print(path)
        cv2.imwrite(path, res)
        image = Image.open(path)
        a = np.asarray(image)
        a = cv2.cvtColor(a, cv2.COLOR_RGB2BGR)
        blur = cv2.bilateralFilter(a,9,75,75)
        pix = image.load()
        lower = np.array(pix[x_cor, y_cor])
        upper = np.array(pix[x_cor, y_cor])
        print(upper)
        mask = np.zeros
        I = np.array(blur)
        I = cv2.cvtColor(I, cv2.COLOR_BGR2RGB)
        mask = cv2.inRange(I, lower, upper)
        cv2.imwrite(ground_truth_dest+'\\'+img_name , mask)
        print('done writing, ', length - counter, ' left')



ns = 4
compact = 1
sigma = 5.0
source = 'C:\challenge\Superpixel\scaled'
super_pixel_dest = 'C:\challenge\Superpixel\super_pixeled'
ground_truth_dest = 'C:\challenge\Superpixel\segmented'

wb = Workbook()
sheet1 = wb.add_sheet('Sheet 1')
sheet1.write(0,0,'Image name')
sheet1.write(0,1,'X')
sheet1.write(0,2,'Y')
sheet1.col(0).width = 5000

names = listdir(source)
i=1
counter=1
length = len(names)
for img_name in names:
    a = cv2.imread(source+'\\'+img_name, 1)
    img = Image.open(source+'\\'+img_name)
    sheet1.write(i, 0, img_name)
    width, height = img.size
    img1 = img_as_float(a)

    cv2.imshow('SuperPixel', img1)
    cv2.setMouseCallback('SuperPixel', click_event, param=i)
    cv2.waitKey(0)
    i+=1

    counter+=1


wb.save('C:\challenge\Superpixel\mole_coordinates.xls')
















