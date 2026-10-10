import SwiftUI
import UIKit

/// AWERO design tokens, measured from the owner's light and dark reference screens
/// (`docs/design/screens`, `docs/design/screens/dark`). Every color follows the system
/// appearance, so views use these tokens instead of literal colors.
///
/// Names keep their historical roles: `ivory` is the page background and `navy` the primary
/// text color in both themes.
enum AweroDesign {
    /// Page background.
    static let ivory = dynamic(light: 0xFBF6EF, dark: 0x121624)
    /// Primary text and icons.
    static let navy = dynamic(light: 0x1D2433, dark: 0xF2EDE6)
    /// Secondary text.
    static let textSecondary = dynamic(light: 0x6E6B6B, dark: 0xA39D97)
    /// Accent for icons, selection outlines, the selected tab and progress rings.
    static let coral = Color(hex: 0xF26A3D)
    /// Filled controls that carry white text (primary buttons, selected day chips, the ✓ key).
    /// Darker than `coral` so white labels meet contrast.
    static let coralStrong = Color(hex: 0xE0502F)
    /// Selected mission card and soft accent fills.
    static let coralSoft = dynamic(light: 0xFDEEE6, dark: 0x2E2326)
    /// Positive state: completed days, "scheduled".
    static let sage = Color(hex: 0x6AA84F)
    static let successSoft = dynamic(light: 0xE9F0DF, dark: 0x1F2E24)
    static let successText = dynamic(light: 0x3F8A3A, dark: 0x7FC36A)
    /// Cards, list groups and keypad keys.
    static let surface = dynamic(light: 0xFFFFFF, dark: 0x1E2335)
    /// Recessed panels such as the time wheel and stat tiles.
    static let surfaceMuted = dynamic(light: 0xF4ECE2, dark: 0x1E2335)
    /// Selection band inside the time wheel.
    static let surfaceStrong = dynamic(light: 0xECE2D6, dark: 0x2B3247)
    /// Unselected chips and switch tracks.
    static let chip = dynamic(light: 0xEFE8DF, dark: 0x262C40)
    /// Tip and notice cards.
    static let surfaceWarm = dynamic(light: 0xFDF0DC, dark: 0x2E2A1F)
    /// Destructive and emergency text.
    static let warning = dynamic(light: 0xB5241D, dark: 0xF0645A)
    static let warningSoft = dynamic(light: 0xFBE9E5, dark: 0x2E1F22)
    static let warningLine = dynamic(light: 0xF2C9C2, dark: 0x5A2C2C)
    /// Sun glyphs in greetings and alarm rows.
    static let sun = Color(hex: 0xF5A623)
    static let border = dynamic(light: 0x1D2433, dark: 0xF2EDE6, alpha: 0.10)

    enum Space {
        static let compact: CGFloat = 8
        static let section: CGFloat = 16
        static let page: CGFloat = 20
        static let card: CGFloat = 22
    }

    enum Corner {
        static let control: CGFloat = 14
        static let card: CGFloat = 22
        static let panel: CGFloat = 24
    }

    static let minimumTouchTarget: CGFloat = 44

    private static func dynamic(light: UInt32, dark: UInt32, alpha: CGFloat = 1) -> Color {
        Color(UIColor { traits in
            UIColor(hex: traits.userInterfaceStyle == .dark ? dark : light, alpha: alpha)
        })
    }
}

extension Color {
    init(hex: UInt32, alpha: Double = 1) {
        self.init(UIColor(hex: hex, alpha: CGFloat(alpha)))
    }
}

extension UIColor {
    convenience init(hex: UInt32, alpha: CGFloat = 1) {
        self.init(
            red: CGFloat((hex >> 16) & 0xFF) / 255,
            green: CGFloat((hex >> 8) & 0xFF) / 255,
            blue: CGFloat(hex & 0xFF) / 255,
            alpha: alpha
        )
    }
}
