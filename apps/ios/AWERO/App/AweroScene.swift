import SwiftUI
import UIKit

/// Mountain-lake photo scenes used as AWERO backgrounds. The Home band follows the time of day;
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

    /// Portrait dawn photo for the wake and mission screens.
    static let wakeImage: UIImage? = load("scene-dawn-portrait")

    // The photos ship as loose JPEGs, which `Image(_ name:)` cannot resolve, so they are loaded by
    // path once and cached. A missing file falls back to the drawn sunrise.
    private static let bandImages: [AweroScene: UIImage?] = Dictionary(
        uniqueKeysWithValues: allCases.map { ($0, load("scene-\($0.rawValue)-band")) }
    )

    private static func load(_ name: String) -> UIImage? {
        Bundle.main.path(forResource: name, ofType: "jpg").flatMap(UIImage.init(contentsOfFile:))
    }
}

/// Full-width photo header for the wake screen; fades into the ivory surface at the bottom and
/// lightens the top so the navy title stays readable over the sky.
struct WakeSceneBackground: View {
    var height: CGFloat

    var body: some View {
        Group {
            if let image = AweroScene.wakeImage {
                Image(uiImage: image)
                    .resizable()
                    .scaledToFill()
                    .frame(height: height)
                    .frame(maxWidth: .infinity)
                    .clipped()
                    .overlay(alignment: .top) {
                        LinearGradient(
                            colors: [AweroDesign.ivory.opacity(0.70), AweroDesign.ivory.opacity(0)],
                            startPoint: .top,
                            endPoint: .bottom
                        )
                        .frame(height: height * 0.55)
                    }
            } else {
                SunriseArtwork(height: height, showsForest: true, showsLake: true)
            }
        }
        .accessibilityHidden(true)
    }
}
