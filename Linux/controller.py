import socket
import sys
import subprocess
import time


def mapValues(vx, v1, v2, n1, n2):
    # v1 start of range, v2 end of range, vx the starting number
    percentage = (vx - v1) / (v2 - v1)
    # n1 start of new range, n2 end of new range
    return (n2 - n1) * percentage + n1


def constrain(value, minVal, maxVal):
    if value < minVal:
        value = minVal
    if value > maxVal:
        value = maxVal
    return value


def normalizeValues(aX, aY):
    # Resting zone midpoint, default 0
    startX = 90
    startY = 0
    # Resting zone is start +- sMin
    sMinX = 10.0
    sMinY = 10.0
    # Maximum angle considered
    sMaxX = 30.0
    sMaxY = 30.0
    # Map from angles to aX values between -1 and 1
    if aX < startX - sMinX:
        aX = mapValues(aX, startX - sMinX, startX - sMaxX, 0.0, 1.0)
    elif aX > startX + sMinX:
        aX = mapValues(aX, startX + sMinX, startX + sMaxX, 0.0, -1.0)
    else:
        aX = 0
    if aY < startY - sMinY:
        aY = mapValues(aY, startY - sMinY, startY - sMaxY, 0.0, 1.0)
    elif aY > startY + sMinY:
        aY = mapValues(aY, startY + sMinY, startY + sMaxY, 0.0, -1.0)
    else:
        aY = 0
    # Limit maximum values in all axes
    aX = constrain(aX, -1, 1)
    aY = constrain(aY, -1, 1)
    return (aX, aY)


# Create socket
socketClient = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
# Connect the socket to the port where the server is listening
serverAddress = ('192.168.42.1', 3005)
print ('Connecting to server')
socketClient.connect(serverAddress)
print ('Connected to server')

delay = 0.200

try:
    while True:
        angleX = subprocess.check_output(
             "cat /sys/bus/iio/devices/iio:device*/in_incli_x_raw", shell=True)
        angleY = subprocess.check_output(
             "cat /sys/bus/iio/devices/iio:device*/in_incli_y_raw", shell=True)
        angleX = float(angleX)/100000
        angleY = float(angleY)/100000
        speed, angle = normalizeValues(angleX, angleY)
        msg = '{:+.2f} {:+.2f};'.format(-angle, speed)
        print(msg)
        socketClient.sendall(msg)
        time.sleep(delay)

finally:
    print('Closing socket')
    socketClient.send('quit')
    socketClient.close()
