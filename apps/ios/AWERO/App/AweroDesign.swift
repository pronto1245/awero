import SwiftUI

/// Shared light palette for the approved AWERO concept.
enum AweroDesign {
    static let ivory = Color(red: 1, green: 248.0 / 255, blue: 239.0 / 255)
    static let navy = Color(red: 20.0 / 255, green: 41.0 / 255, blue: 75.0 / 255)
    static let coral = Color(red: 1, green: 104.0 / 255, blue: 75.0 / 255)
    static let sage = Color(red: 79.0 / 255, green: 139.0 / 255, blue: 102.0 / 255)
    static let surface = Color.white
    static let surfaceWarm = Color(red: 1, green: 0.95, blue: 0.88)
    static let warning = Color(red: 0.83, green: 0.19, blue: 0.24)
    static let border = navy.opacity(0.10)

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
}
