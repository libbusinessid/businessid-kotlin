// Copyright The EntID Authors.
// SPDX-License-Identifier: Apache-2.0

package org.entid.internal

import org.entid.ValidationProfile
import org.entid.runtime.CpView

/**
 * Everything a format or checksum program may read besides its own subject.
 *
 * @property value the canonical value of the identifier under validation, what
 *   `value()` yields even inside a called program.
 * @property profile the resolved profile, what `profile_is` compares against.
 * @property target the selected dispatch target, what `country_code()` reads.
 */
internal class EvalContext(
    @JvmField val value: CpView,
    @JvmField val profile: ValidationProfile,
    @JvmField val target: Int,
)
