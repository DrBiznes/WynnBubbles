# Notices

WynnBubbles is licensed under the GNU Lesser General Public License v3.0 (see `LICENSE`, which
supplements the GNU General Public License v3.0 in `LICENSE.GPL`).

## TalkBubbles

WynnBubbles started as a fork of [TalkBubbles](https://github.com/Globox1997/TalkBubbles) by
Globox_Z, and parts of that code remain. TalkBubbles is distributed under the MIT License:

```
MIT License

Copyright (c) 2021 Globox_Z

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

## Talk Balloons

The stacked-bubble behaviour (several bubbles per player, newest at the bottom, each expiring on
its own) follows [Talk Balloons](https://github.com/CERBON-MODS/Talk-Balloons) by CerbonXD and
BluSpring, licensed under LGPL-3.0.

## Wynntils

Player identity (ghost UUID handling, NPC detection) is read from
[Wynntils](https://github.com/Wynntils/Wynntils) when it is installed, and the ghost UUID
conversion in `PlayerIds` mirrors Wynntils' `PlayerModel#getUserUUID`. Wynntils is licensed under
LGPL-3.0. It is an optional dependency and is not bundled.
