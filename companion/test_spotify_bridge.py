import unittest

import spotify_bridge as bridge

SEP = bridge.SEP


class ParseStateTest(unittest.TestCase):
    def test_not_running(self):
        state = bridge.parse_state("notrunning")
        self.assertFalse(state["running"])
        self.assertFalse(state["isPlaying"])

    def test_stopped_is_running_without_a_track(self):
        state = bridge.parse_state("stopped")
        self.assertTrue(state["running"])
        self.assertEqual(state["title"], "")

    def test_playing_track(self):
        raw = SEP.join(["playing", "Nights", "Frank Ocean", "Blonde", "307000", "spotify:track:abc",
                        "https://i.scdn.co/image/x", "102.5", "true", "false"]) + "\n"
        state = bridge.parse_state(raw, "MacBook")
        self.assertTrue(state["isPlaying"])
        self.assertEqual(state["title"], "Nights")
        self.assertEqual(state["artist"], "Frank Ocean")
        self.assertEqual(state["durationMs"], 307000)
        self.assertEqual(state["positionMs"], 102500)
        self.assertTrue(state["shuffle"])
        self.assertFalse(state["repeat"])
        self.assertEqual(state["host"], "MacBook")

    def test_paused_track_keeps_its_details(self):
        raw = SEP.join(["paused", "Song", "Artist", "Album", "1000", "id", "", "0", "false", "true"])
        state = bridge.parse_state(raw)
        self.assertFalse(state["isPlaying"])
        self.assertEqual(state["title"], "Song")
        self.assertTrue(state["repeat"])

    def test_comma_decimal_separator_from_other_locales(self):
        raw = SEP.join(["playing", "S", "A", "B", "1000", "id", "", "12,5", "false", "false"])
        self.assertEqual(bridge.parse_state(raw)["positionMs"], 12500)

    def test_malformed_output_does_not_crash(self):
        self.assertEqual(bridge.parse_state("garbage")["title"], "")


if __name__ == "__main__":
    unittest.main()
