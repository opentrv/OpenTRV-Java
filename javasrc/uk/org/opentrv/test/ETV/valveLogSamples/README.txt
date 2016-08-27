Contains logs for OpenTRV valves associated with boiler controller 2d1a in 2016H1:
    0a45 0d49 2d1a 3015 414a
and one synthetic partially-decrypted-format log:
    synthd

These are for the house at 16WW, identified in the N-format bulk data as "5013".

Note that 2d1a is here a boiler controller and controlled its own valve.

Note that all these 'valves' were non-REV7 split units with a REV1/REV2/REV4 controller
driving a FS20 FTH8V third-party valve head.

The filesystem layout is:
    <contoller1ID>/<valve1ID>.{json,dlog}[.gz]
    <contoller1ID>/<valve2ID>.{json,dlog}[.gz]
...
    <contoller1ID>/<valveNID>.{json,dlog}[.gz]

The controllerID may be a house ID instead if there is no explicitly-recorded
boiler controller or relay, eg if relaying data at campus level over LoRaWAN.

The plain .log format is JSON one line per record:
    [ "2016-03-31T05:18:45Z", "", {"@":"3015","+":1,"v|%":0,"tT|C":14,"tS|C":4} ]
or a partially-decrypted format:
    '2016-05-12-11:21:45','111.11.11.1','cf 74 II II II II 20 0b 40 09 d8 59 0a e5 75 f3 13 57 a5 94 a2 3b e7 26 99 c4 5a 77 74 6a 6e 2c 5a c2 22 f6 b6 5e 0b 02 31 f2 09 45 57 d4 d9 92 3c 8e 45 95 63 65 5b a3 ff 2f 3d 68 14 80','b''\x00\x10{"tT|C":21,"tS|C":1''','\x00\x10{"tT|C":21,"tS|C":1'

The .log.gz format is GZIPped to save space.
