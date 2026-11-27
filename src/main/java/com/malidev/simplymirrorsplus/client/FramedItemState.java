//? if >=1.21.4 {
package com.malidev.simplymirrorsplus.client;

import com.malidev.simplymirrorsplus.block.MirrorBlock;

/** What the mod adds to the game's item render state: the frame of a framed mirror, or null. */
public interface FramedItemState {
    void simplymirrorsplus$frame(String frame,MirrorBlock.Form form);
}
//?}
