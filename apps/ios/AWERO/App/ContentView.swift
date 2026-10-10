import SwiftUI

struct ContentView: View {
    @EnvironmentObject private var wakeFlow: WakeFlowController
    @EnvironmentObject private var alarms: AlarmStore
    @AppStorage("awero.didCompleteOnboarding.v1") private var didCompleteOnboarding = false
    @State private var showingFirstAlarm = false

    var body: some View {
        Group {
            switch wakeFlow.state {
            case .ringing, .mission, .completed, .emergencyStopped, .storageError:
                WakeScreen(flow: wakeFlow)
            case .idle:
                switch alarms.loadState {
                case .loading:
                    ProgressView()
                case .failed:
                    HomeView(showingCreate: $showingFirstAlarm)
                case .loaded where didCompleteOnboarding || !alarms.alarms.isEmpty:
                    HomeView(showingCreate: $showingFirstAlarm)
                        .onAppear { didCompleteOnboarding = true }
                case .loaded:
                    OnboardingView {
                        didCompleteOnboarding = true
                        showingFirstAlarm = true
                    }
                }
            }
        }
        .sheet(isPresented: $showingFirstAlarm) {
            CreateAlarmView()
        }
    }
}
