/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.pranav.ide.dex

import com.pranav.ide.dex.util.Unsigned.compare

/**
 * A call_site_id_item: https://source.android.com/devices/tech/dalvik/dex-format#call-site-id-item
 */
class CallSiteId(private val dex: Dex?, val callSiteOffset: Int) : Comparable<CallSiteId> {
    override fun compareTo(other: CallSiteId): Int {
        return compare(callSiteOffset, other.callSiteOffset)
    }

    fun writeTo(out: Dex.Section) {
        out.writeInt(callSiteOffset)
    }

    override fun toString(): String {
        return dex?.protoIds()?.get(callSiteOffset)?.toString() ?: callSiteOffset.toString()
    }
}
