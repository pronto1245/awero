import SwiftUI


struct OnboardingView: View {
    let onContinue: () -> Void
    @ScaledMetric(relativeTo: .largeTitle) private var titleSize: CGFloat = 34

    var body: some View {
        ZStack {
            AweroDesign.ivory.ignoresSafeArea()
            ScrollView {
                VStack(alignment: .leading, spacing: 20) {
                    HStack {
                        Text("AWERO")
                            .font(.system(size: 26, weight: .black))
                            .foregroundStyle(AweroDesign.navy)
                        Spacer()
                        Image(systemName: "sun.max.fill")
                            .foregroundStyle(Color(red: 1, green: 0.64, blue: 0.19))
                    }
                    SunriseArtwork(height: 174, cornerRadius: 20)
                    Text("onboarding.title")
                        .font(.system(size: titleSize, weight: .bold))
                        .foregroundStyle(AweroDesign.navy)
                        .accessibilityAddTraits(.isHeader)
                        .accessibilityIdentifier("onboarding.title")
                    Text("onboarding.body")
                        .font(.body)
                        .foregroundStyle(AweroDesign.navy.opacity(0.75))
                    VStack(alignment: .leading, spacing: 12) {
                        Label("onboarding.privacy", systemImage: "lock.shield")
                            .font(.subheadline)
                            .foregroundStyle(AweroDesign.navy.opacity(0.78))
                        Text("onboarding.permissions")
                            .font(.subheadline)
                            .foregroundStyle(AweroDesign.navy.opacity(0.72))
                    }
                    .padding(18)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(.white.opacity(0.86), in: RoundedRectangle(cornerRadius: 20))
                }
                .padding(.horizontal, 22)
                .padding(.top, 16)
                .padding(.bottom, 16)
            }
            .safeAreaInset(edge: .bottom) {
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
                .padding(.horizontal, 20)
                .padding(.vertical, 8)
                .background(AweroDesign.ivory.opacity(0.96))
            }
        }
    }
}
