# Mapping-Robot
A subsumption-architecture based robot built with using the lejos ev3 API.
The task is to build an occupancy grid of the arena and display it on the UI/robot LCD.

## Environment Configuration
- An arena of 7x6 cells in size, where each cell is 25x25cm.
- The arena will have a a number of obstacles blocks at random locations. These obstacles will be 25x25x10cm in dimension. 
- Each of the four corner squares will have a unique colour that can be detected as a beacon, to assist with localization.

## Robot Configuration
- A 4 wheeled lego car.
- Two bumper sensors at the front of the chasis.
- One gyro sensor in the middle (beneath) of the chasis.
- One ultrasound at the front of the car linked to a rotational motor.
