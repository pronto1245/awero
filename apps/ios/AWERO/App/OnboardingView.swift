import SwiftUI

private enum OnboardingStyle {
    static let ivory = Color(red: 1.0, green: 0.973, blue: 0.937)
    static let navy = Color(red: 0.078, green: 0.161, blue: 0.294)
    static let coral = Color(red: 1.0, green: 0.408, blue: 0.294)
}

struct OnboardingView: View {
    let onContinue: () -> Void
    @ScaledMetric(relativeTo: .largeTitle) private var titleSize: CGFloat = 34

    var body: some View {
        ZStack {
            OnboardingStyle.ivory.ignoresSafeArea()
            ScrollView {
                VStack(alignment: .leading, spacing: 20) {
                    Text("AWERO")
                        .font(.title.bold())
                        .foregroundStyle(OnboardingStyle.navy)
                    Text("onboarding.title")
                        .font(.system(size: titleSize, weight: .bold, design: .rounded))
                        .foregroundStyle(OnboardingStyle.navy)
                        .accessibilityAddTraits(.isHeader)
                        .accessibilityIdentifier("onboarding.title")
                    Text("onboarding.body")
                        .font(.body)
                        .foregroundStyle(OnboardingStyle.navy.opacity(0.75))
                    Text("onboarding.privacy")
                        .font(.body)
                        .foregroundStyle(OnboardingStyle.navy.opacity(0.75))
                    Text("onboarding.permissions")
                        .font(.body)
                        .foregroundStyle(OnboardingStyle.navy.opacity(0.75))
                    Spacer(minLength: 24)
                    Button(action: onContinue) {
                        Text("onboarding.create_alarm")
                            .font(.headline)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 16)
                            .background(OnboardingStyle.coral)
                            .foregroundStyle(.white)
                            .clipShape(RoundedRectangle(cornerRadius: 18))
                    }
                    .accessibilityHint(Text("onboarding.create_alarm_hint"))
                    .accessibilityIdentifier("onboarding.createAlarm")
                }
                .padding(24)
            }
        }
    }
}
