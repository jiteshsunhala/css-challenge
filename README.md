```
How to run the program.

* Use cd to get to the direcotry where Dockerfile exists.
* Run docker build -t kitchen-application .
* Once the image build works, you can run:
** docker run kitchen-application placeRate pickupRateMin pickupRateMax

Note: placeRate, pickupRateMin, pickupRateMax - all should be in microseconds. 
```