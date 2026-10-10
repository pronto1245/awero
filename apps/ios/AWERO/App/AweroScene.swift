import SwiftUI
import UIKit

/// Illustrated mountain-lake scenes used as AWERO backgrounds. The Home band follows the time of day;
/// the wake screen always uses the dawn scene from the approved design.
enum AweroScene: String, CaseIterable {
    case dawn, day, sunset, night

    /// Dawn 05–11, day 11–18, sunset 18–23, night 23–05 (device local time).
    static func current(at date: Date = .now, calendar: Calendar = .current) -> AweroScene {
        switch calendar.component(.hour, from: date) {
        case 5..<11: return .dawn
        case 11..<18: return .day
        case 18..<23: return .sunset
        default: return .night
        }
    }

    /// Wide crop for the Home header band.
    var bandImage: UIImage? { Self.bandImages[self] ?? nil }

    /// Portrait dawn scene for the wake and mission screens.
    static let wakeImage: UIImage? = load("scene-dawn-portrait")

    // The scenes ship as loose JPEGs, which `Image(_ name:)` cannot resolve, so they are loaded by
    // path once and cached. A missing file falls back to the drawn sunrise.
    private static let bandImages: [AweroScene: UIImage?] = Dictionary(
        uniqueKeysWithValues: allCases.map { ($0, load("scene-\($0.rawValue)-band")) }
    )

    private static func load(_ name: String) -> UIImage? {
        Bundle.main.path(forResource: name, ofType: "jpg").flatMap(UIImage.init(contentsOfFile:))
    }
}

/// Full-screen illustrated dawn scene behind the wake and mission screens. The artwork already
/// fades into the ivory surface at the bottom, so it sits directly under the mission controls.
struct WakeSceneBackground: View {
    var body: some View {
        Group {
            if let image = AweroScene.wakeImage {
                Image(uiImage: image)
                    .resizable()
                    .scaledToFill()
                    .frame(minWidth: 0, maxWidth: .infinity, minHeight: 0, maxHeight: .infinity)
                    .clipped()
            } else {
                VStack(spacing: 0) {
                    SunriseArtwork(height: 365, showsForest: true, showsLake: true)
                    AweroDesign.ivory
                }
            }
        }
        .accessibilityHidden(true)
    }
}
