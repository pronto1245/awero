import SwiftUI


struct OnboardingView: View {
    let onContinue: () -> Void
    @ScaledMetric(relativeTo: .largeTitle) private var titleSize: CGFloat = 34

    var body: some View {
        ZStack {
            AweroDesign.ivory.ignoresSafeArea()
            ScrollView {
                VStack(alignment: .leading, spacing: 20) {
                    Text("AWERO")
                        .font(.title.bold())
                        .foregroundStyle(AweroDesign.navy)
                    Text("onboarding.title")
                        .font(.system(size: titleSize, weight: .bold, design: .rounded))
                        .foregroundStyle(AweroDesign.navy)
                        .accessibilityAddTraits(.isHeader)
                        .accessibilityIdentifier("onboarding.title")
                    Text("onboarding.body")
                        .font(.body)
                        .foregroundStyle(AweroDesign.navy.opacity(0.75))
                    Text("onboarding.privacy")
                        .font(.body)
                        .foregroundStyle(AweroDesign.navy.opacity(0.75))
                    Text("onboarding.permissions")
                        .font(.body)
                        .foregroundStyle(AweroDesign.navy.opacity(0.75))
                    Spacer(minLength: 24)
                    Button(action: onContinue) {
                        Text("onboarding.create_alarm")
                            .font(.headline)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 16)
                            .background(AweroDesign.coral)
                            .foregroundStyle(AweroDesign.navy)
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
