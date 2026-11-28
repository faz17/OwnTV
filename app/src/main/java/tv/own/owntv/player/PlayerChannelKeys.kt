package tv.own.owntv.player

import tv.own.owntv.core.settings.PlayerRemoteOptions

/**
 * The zap step for the physical CH+/CH− keys. The optional swap preference applies
 * to those keys, not the directional D-pad: Down always follows the next visible row.
 */
fun zapDirection(up: Boolean, options: PlayerRemoteOptions): Int = if (up != options.swapChannelKeys) 1 else -1

/** Fire TV D-pad navigation follows the on-screen list: Down = next (+1), Up = previous (-1). */
fun dpadZapDirection(up: Boolean): Int = if (up) -1 else 1

/** Whether number keys type a channel number: the Channel numbers setting and "Number keys tune channels" both on. */
fun numberKeysTune(channelNumbersOn: Boolean, options: PlayerRemoteOptions): Boolean =
    channelNumbersOn && options.numberKeysTune
