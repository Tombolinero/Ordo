package io.github.tombolinero.ordo.core.ui

import io.github.tombolinero.ordo.core.designsystem.component.PillowStyle
import io.github.tombolinero.ordo.core.model.ListColor

fun ListColor.pillow(): PillowStyle = when (this) {
    ListColor.Graphite -> PillowStyle.Graphite
    ListColor.Magenta -> PillowStyle.Magenta
    ListColor.Violet -> PillowStyle.Violet
    ListColor.Ruby -> PillowStyle.Ruby
    ListColor.Cobalt -> PillowStyle.Cobalt
    ListColor.Petrol -> PillowStyle.Petrol
}

fun ListColor.labelRes(): Int = when (this) {
    ListColor.Graphite -> R.string.color_graphite
    ListColor.Magenta -> R.string.color_magenta
    ListColor.Violet -> R.string.color_violet
    ListColor.Ruby -> R.string.color_ruby
    ListColor.Cobalt -> R.string.color_cobalt
    ListColor.Petrol -> R.string.color_petrol
}
