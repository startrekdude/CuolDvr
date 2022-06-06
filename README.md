# [Cuol](https://web.archive.org/web/20180717182015/https://carleton.ca/cuol/)Dvr

## Motivation

In the pre-COVID times, [Carleton University](https://carleton.ca/) offered online classes through <abbr title="Carleton University OnLine">CUOL</abbr>. Students could watch their lectures live, at predetermined times on the CUOL Web Channel, or register in the `[A-Z]OD` section for a fee of $50 per course to download and stream lectures on demand.

If you're anything like me, your first thought on reading that was "Why not just record the live broadcasts and skip paying the $50?" Or, rather: "I don't want to be awake at 08h30 uh...*ever*...so why not make my computer record it?" And thus, CuolDvr was born. I anticipated saving a significant amount of money over the course of my degree as I planned to take a majority of my electives online.

CuolDvr was completed in February 2020.

Subsequently, the Black Swan event of my generation occurred<!--fingers crossed-->, and *all* classes were moved online. Carleton's whole system for online courses was revamped to rely less on classrooms outfitted for video recording and more on off-the-shelf tools like Zoom. Carleton no longer charged extra for on-demand access to lecture recordings.

As the Fall 2022 schedule was just released, and there is no indication Carleton is returning to the old way, I am releasing CuolDvr as open source code. So much for saving hundreds of dollars, but maybe someone will find it useful.

## Uses & usage

Despite being developed for a rather specific purpose, CuolDvr can act as a fairly generic DVR for [HLS](https://datatracker.ietf.org/doc/html/rfc8216) streams. That is, given a `.m3u8` playlist URL and a schedule of times to record, it'll automatically make you nice `.mp4` files of your programs. See the `streams.cfg` and `record.cfg` files for a model configuration. It pretends to be IE 11 on Windows 8.1—probably someone should change that.

Perhaps more usefully, it has some novel techniques for dealing with... I'm not sure whether to say "streams that take certain liberties with the standard" or just "completely corrupt streams." Probably it's a bit of both. It can successfully remux to a single `.mp4` file streams from which the constituent `.ts` segments exhibit any of the following:

1. Different audio/video lengths.
2. Different audio sample rates.
3. Generic corruption; i.e. unreadable by `ffmpeg`.

It can do so without ever re-encoding either audio or video and maintain sync throughout.

Usage: `CuolDvr.jar [-loglevel:LOGLEVEL] <daemon|remux>`

## License

[GNU General Public License, version 3](https://choosealicense.com/licenses/gpl-3.0/)