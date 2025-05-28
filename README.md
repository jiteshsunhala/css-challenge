### How to run the program.

* Use cd to get to the directory where Dockerfile exists.
* Run `docker build -t kitchen-application .`
* Once the image build works, you can run:

```
docker run kitchen-application <placeRate> <pickupRateMin> <pickupRateMax>
```

Note: placeRate, pickupRateMin, pickupRateMax - all should be in microseconds.

Example run will be:

```
docker run kitchen-application 500000 4000000 8000000
```

### Logic to discard orders.
In order to discard orders to be able to place new orders on the room temperature shelf, we will pick an order 
which is closer to getting stale.

Basically, we store all the orders based on their freshness time remaining. Order with the lowest freshness time will
be stored first, and then the one with the highest freshness time.

#### Why this logic ?
Removing orders which are closer to their staleness helps us allow customers to be able to pickup more number of orders.
And, reduces the number of orders discarded.

We want to keep orders on the shelf for pickup which can stay fresh for longer time period.