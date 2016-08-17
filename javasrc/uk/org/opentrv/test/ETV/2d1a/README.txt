Contains logs for OpenTRV valves associated with boiler controller 2d1a in 2016H1.

These are for the house at 16WW, identified in the N-format bulk data as "5013".

Note that 2d1a is here a boiler controller and controlled its own valve.

Note that all these 'valves' were non-REV7 split units with a REV1/REV2/REV4 controller
driving a FS20 FTH8V third-party valve head.

The filesystem layout is:
    <contoller1ID>/<valve1ID>.log[.gz]
    <contoller1ID>/<valve2ID>.log[.gz]
...
    <contoller1ID>/<valveNID>.log[.gz]

The controllerID may be a house ID instead if there is no explicitly-recorded
boiler controller or relay, eg if relaying data at campus level over LoRaWAN.

The plain .log format is JSON one line per record.  The .gz is gzipped to save space.

