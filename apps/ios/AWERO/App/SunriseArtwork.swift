import SwiftUI

struct SunriseArtwork: View {
    var height: CGFloat = 148
    var body: some View {
        GeometryReader { geometry in
            ZStack {
                LinearGradient(
                    colors: [
                        Color(red: 1.0, green: 0.79, blue: 0.63),
                        Color(red: 1.0, green: 0.91, blue: 0.72),
                        Color(red: 0.91, green: 0.85, blue: 0.78)
                    ],
                    startPoint: .top,
                    endPoint: .bottom
                )
                Circle()
                    .fill(Color(red: 1.0, green: 0.77, blue: 0.31))
                    .frame(width: 68, height: 68)
                    .position(x: geometry.size.width * 0.5, y: geometry.size.height * 0.66)
                Canvas { context, size in
                    var distant = Path()
                    distant.move(to: CGPoint(x: 0, y: size.height * 0.72))
                    distant.addLine(to: CGPoint(x: size.width * 0.20, y: size.height * 0.48))
                    distant.addLine(to: CGPoint(x: size.width * 0.39, y: size.height * 0.72))
                    distant.addLine(to: CGPoint(x: size.width * 0.62, y: size.height * 0.42))
                    distant.addLine(to: CGPoint(x: size.width * 0.84, y: size.height * 0.73))
                    distant.addLine(to: CGPoint(x: size.width, y: size.height * 0.57))
                    distant.addLine(to: CGPoint(x: size.width, y: size.height))
                    distant.addLine(to: CGPoint(x: 0, y: size.height))
                    distant.closeSubpath()
                    context.fill(distant, with: .color(Color(red: 0.54, green: 0.61, blue: 0.67)))

                    var foreground = Path()
                    foreground.move(to: CGPoint(x: 0, y: size.height * 0.83))
                    foreground.addCurve(
                        to: CGPoint(x: size.width, y: size.height * 0.82),
                        control1: CGPoint(x: size.width * 0.28, y: size.height * 0.68),
                        control2: CGPoint(x: size.width * 0.67, y: size.height * 0.96)
                    )
                    foreground.addLine(to: CGPoint(x: size.width, y: size.height))
                    foreground.addLine(to: CGPoint(x: 0, y: size.height))
                    foreground.closeSubpath()
                    context.fill(foreground, with: .color(Color(red: 0.34, green: 0.48, blue: 0.46)))
                }
            }
            .clipShape(RoundedRectangle(cornerRadius: 22))
        }
        .frame(height: height)
        .accessibilityHidden(true)
    }
}
